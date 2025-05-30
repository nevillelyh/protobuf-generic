package me.lyh.protobuf.generic

import java.io.{InputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer

import com.google.protobuf.{ByteString, CodedInputStream, WireFormat}
import com.google.protobuf.Descriptors.FieldDescriptor.Type

object FieldReader {
  def of(schema: Schema, fields: Seq[String]): FieldReader = new FieldReader(schema, fields)
}

class FieldReader(val schema: Schema, val fields: Seq[String]) extends Serializable {
  private val (idxMap, defaults) = {
    val xs = fields.map(prepareField)
    (xs.map(_._1).zipWithIndex.toMap, xs.map(_._2))
  }

  def read(buf: Array[Byte]): Array[Any] = read(CodedInputStream.newInstance(buf))

  def read(buf: ByteBuffer): Array[Any] = read(CodedInputStream.newInstance(buf))

  def read(input: InputStream): Array[Any] = read(CodedInputStream.newInstance(input))

  private def read(input: CodedInputStream): Array[Any] = {
    val result = defaults.toArray
    read(input, schema.root, Nil, result)
    result
  }

  private def read(
    input: CodedInputStream,
    messageSchema: MessageSchema,
    ids: List[Int],
    result: Array[Any]
  ): Unit = {
    while (!input.isAtEnd) {
      val tag = input.readTag()
      val id = WireFormat.getTagFieldNumber(tag)
      val field = messageSchema.fields(id)

      if (field.label == Label.REPEATED) {
        if (field.packed) {
          val bytesIn = CodedInputStream.newInstance(input.readByteBuffer())
          while (!bytesIn.isAtEnd) {
            readValue(bytesIn, field, ids, result, true)
          }
        } else {
          readValue(input, field, ids, result, true)
        }
      } else {
        val value = readValue(input, field, ids, result, false)
        idxMap.get(id :: ids).foreach(i => result(i) = value)
      }
    }
  }

  private def readValue(
    in: CodedInputStream,
    field: Field,
    ids: List[Int],
    result: Array[Any],
    discard: Boolean
  ): Any = field.`type` match {
    case Type.FLOAT    => in.readFloat()
    case Type.DOUBLE   => in.readDouble()
    case Type.FIXED32  => in.readFixed32()
    case Type.FIXED64  => in.readFixed64()
    case Type.INT32    => in.readInt32()
    case Type.INT64    => in.readInt64()
    case Type.UINT32   => in.readUInt32()
    case Type.UINT64   => in.readUInt64()
    case Type.SFIXED32 => in.readSFixed32()
    case Type.SFIXED64 => in.readSFixed64()
    case Type.SINT32   => in.readSInt32()
    case Type.SINT64   => in.readSInt64()
    case Type.BOOL     => in.readBool()
    case Type.STRING   => in.readString()
    case Type.BYTES    =>
      if (discard) {
        in.skipRawBytes(in.readRawVarint32())
        null
      } else {
        in.readBytes()
      }
    case Type.ENUM =>
      val enum = in.readEnum()
      if (discard) null else schema.enums(field.schema.get).values(enum)
    case Type.MESSAGE =>
      if (discard) {
        in.skipRawBytes(in.readRawVarint32())
        null
      } else {
        val nestedIn = CodedInputStream.newInstance(in.readByteBuffer())
        read(nestedIn, schema.messages(field.schema.get), field.id :: ids, result)
      }
    case Type.GROUP => throw new IllegalArgumentException("Unsupported type: GROUP")
  }

  /**
   * Field path e.g. "a.b.c" to reverse ids e.g. `3 :: 2 :: 1 :: Nil` and default value.
   */
  private def prepareField(field: String): (List[Int], Any) = {
    val path = field.split('.')
    var ids = List.empty[Int]
    var msgSchema = schema.root
    var i = 0
    var default: Any = null
    while (i < path.length) {
      val name = path(i)
      msgSchema.fields.find(_._2.name == name) match {
        case Some((id, fd)) =>
          require(fd.label != Label.REPEATED, "Repeated field not supported")
          if (i < path.length - 1) {
            require(fd.`type` == Type.MESSAGE, s"Invalid field $field, $name is not a message")
            msgSchema = schema.messages(fd.schema.get)
          } else {
            default = getDefault(fd)
          }
          ids = id :: ids
        case None =>
          throw new IllegalArgumentException(s"Invalid field $field, $name not found")
      }
      i += 1
    }
    (ids, default)
  }

  private def getDefault(field: Field): Any = field.default match {
    case Some(v) => v
    case None    =>
      field.`type` match {
        case Type.FLOAT    => 0.0f
        case Type.DOUBLE   => 0.0
        case Type.FIXED32  => 0
        case Type.FIXED64  => 0L
        case Type.INT32    => 0
        case Type.INT64    => 0L
        case Type.UINT32   => 0
        case Type.UINT64   => 0L
        case Type.SFIXED32 => 0
        case Type.SFIXED64 => 0L
        case Type.SINT32   => 0
        case Type.SINT64   => 0L
        case Type.BOOL     => false
        case Type.STRING   => ""
        case Type.BYTES    => ByteString.EMPTY
        case Type.ENUM     => schema.enums(field.schema.get).values(0)
        case t             => throw new IllegalArgumentException(s"Unsupported type: $t")
      }
  }

  private def readObject(in: ObjectInputStream): Unit = {
    def set(name: String, value: Any): Unit = {
      val f = getClass.getDeclaredField(name)
      f.setAccessible(true)
      f.set(this, value)
    }

    val schema = Schema.fromJson(in.readUTF())
    val fields = (1 to in.readInt()).map(_ => in.readUTF())

    set("schema", schema)
    set("fields", fields)

    val (idxMap, defaults) = {
      val xs = fields.map(prepareField)
      (xs.map(_._1).zipWithIndex.toMap, xs.map(_._2))
    }
    set("idxMap", idxMap)
    set("defaults", defaults)
  }

  private def writeObject(out: ObjectOutputStream): Unit = {
    out.writeUTF(schema.toJson)
    out.writeInt(fields.size)
    fields.foreach(out.writeUTF)
  }
}
