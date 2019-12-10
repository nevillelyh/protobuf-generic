package me.lyh.protobuf.generic

import java.io.{InputStream, ObjectInputStream, ObjectOutputStream, OutputStream}
import java.nio.ByteBuffer
import java.util.{ArrayList => JArrayList, LinkedHashMap => JLinkedHashMap, TreeMap => JTreeMap}

import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedInputStream, WireFormat}

import scala.collection.JavaConverters._

object GenericReader {
  def of(schema: Schema): GenericReader = new GenericReader(schema)
}

class GenericReader(val schema: Schema) extends Serializable {
  def read(buf: Array[Byte]): GenericRecord =
    read(CodedInputStream.newInstance(buf), schema.root)

  def read(buf: ByteBuffer): GenericRecord =
    read(CodedInputStream.newInstance(buf), schema.root)

  def read(input: InputStream): GenericRecord =
    read(CodedInputStream.newInstance(input), schema.root)

  private def read(input: CodedInputStream, messageSchema: MessageSchema): GenericRecord = {
    val map = new JTreeMap[java.lang.Integer, Any]()
    while (!input.isAtEnd) {
      val tag = input.readTag()
      val id = WireFormat.getTagFieldNumber(tag)
      val field = messageSchema.fields(id)

      if (field.label == Label.REPEATED) {
        if (!map.containsKey(id)) {
          map.put(id, new JArrayList[Any]())
        }
        val list = map.get(id).asInstanceOf[java.util.ArrayList[Any]]
        if (field.packed) {
          val bytesIn = CodedInputStream.newInstance(input.readByteBuffer())
          while (!bytesIn.isAtEnd) {
            list.add(readValue(bytesIn, field))
          }
        } else {
          list.add(readValue(input, field))
        }
      } else {
        map.put(id, readValue(input, field))
      }
    }

    val result = new JLinkedHashMap[String, Any]()
    map.asScala.foreach(kv => result.put(messageSchema.fields(kv._1).name, kv._2))
    result
  }

  private def readValue(in: CodedInputStream, field: Field): Any = field.`type` match {
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
    case Type.BYTES    => Base64.encode(in.readByteArray())
    case Type.ENUM     => schema.enums(field.schema.get).values(in.readEnum())
    case Type.MESSAGE =>
      val nestedIn = CodedInputStream.newInstance(in.readByteBuffer())
      read(nestedIn, schema.messages(field.schema.get))
    case Type.GROUP => throw new IllegalArgumentException("Unsupported type: GROUP")
  }

  private def readObject(in: ObjectInputStream): Unit = {
    val schema = Schema.fromJson(in.readUTF())

    val schemaField = getClass.getDeclaredField("schema")
    schemaField.setAccessible(true)
    schemaField.set(this, schema)
  }

  private def writeObject(out: ObjectOutputStream): Unit =
    out.writeUTF(schema.toJson)
}
