package me.lyh.protobuf.generic

import java.io.InputStream
import java.nio.ByteBuffer

import com.google.protobuf.{ByteString, CodedInputStream, WireFormat}
import com.google.protobuf.Descriptors.FieldDescriptor.Type

object FieldReader {
  def of(schema: Schema, fields: Seq[String]): FieldReader = new FieldReader(schema, fields)
}

class FieldReader(val schema: Schema, val fields: Seq[String]) {
  private val rootSchema = schema.messages(schema.name)
  private val (idxMap, defaults) = {
    val xs = fields.map(prepareField)
    (xs.map(_._1).zipWithIndex.toMap, xs.map(_._2))
  }

  def read(buf: Array[Byte]): Array[Any] = read(CodedInputStream.newInstance(buf))

  def read(buf: ByteBuffer): Array[Any] = read(CodedInputStream.newInstance(buf))

  def read(input: InputStream): Array[Any] = read(CodedInputStream.newInstance(input))

  private def read(input: CodedInputStream): Array[Any] = {
    val result = defaults.toArray
    read(input, rootSchema, Nil, result)
    result
  }

  private def read(input: CodedInputStream, messageSchema: MessageSchema, ids: List[Int], result: Array[Any]): Unit = {
    def readValue(in: CodedInputStream, field: Field): Any = field.`type` match {
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
      case Type.BYTES    => ByteString.copyFrom(in.readByteArray())
      case Type.ENUM     => schema.enums(field.schema.get).values(in.readEnum())
      case Type.MESSAGE =>
        val nestedIn = CodedInputStream.newInstance(in.readByteBuffer())
        read(nestedIn, schema.messages(field.schema.get), field.id :: ids, result)
      case Type.GROUP => throw new IllegalArgumentException("Unsupported type: GROUP")
    }

    while (!input.isAtEnd) {
      val tag = input.readTag()
      val id = WireFormat.getTagFieldNumber(tag)
      val field = messageSchema.fields(id)

      if (field.label == Label.REPEATED) {
        if (field.packed) {
          val bytesIn = CodedInputStream.newInstance(input.readByteBuffer())
          while (!bytesIn.isAtEnd) {
            readValue(bytesIn, field)
          }
        } else {
          readValue(input, field)
        }
      } else {
        val value = readValue(input, field)
        idxMap.get(id :: ids).foreach(i => result(i) = value)
      }
    }
  }

  /** Field path e.g. "a.b.c" to reverse ids e.g. `3 :: 2 :: 1 :: Nil` and default value. */
  private def prepareField(field: String): (List[Int], Any)  = {
    val path = field.split('.')
    var ids = List.empty[Int]
    var msgSchema = rootSchema
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

  // FIXME: proto2 custom defaults, e.g. `optional int32 a = 0 [default = 1];`
  private def getDefault(field: Field): Any = field.`type` match {
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
    case t => throw new IllegalArgumentException(s"Unsupported type: $t")
  }
}
