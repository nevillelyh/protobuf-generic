package me.lyh.protobuf.generic

import java.io.InputStream
import java.nio.ByteBuffer

import com.google.common.collect.{Lists, Maps}
import com.google.common.io.BaseEncoding
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedInputStream, WireFormat}

import scala.collection.JavaConverters._

object GenericReader {
  def of(schema: Schema): GenericReader = new GenericReader(schema)
}

class GenericReader(val schema: Schema) {

  private val rootSchema = schema.messages(schema.name)

  def read(buf: Array[Byte]): GenericRecord =
    read(CodedInputStream.newInstance(buf), rootSchema)

  def read(buf: ByteBuffer): GenericRecord =
    read(CodedInputStream.newInstance(buf), rootSchema)

  def read(input: InputStream): GenericRecord =
    read(CodedInputStream.newInstance(input), rootSchema)

  private def read(input: CodedInputStream, messageSchema: MessageSchema): GenericRecord = {
    def readValue(in: CodedInputStream, field: Field): Any = field.`type` match {
      case Type.FLOAT => in.readFloat()
      case Type.DOUBLE => in.readDouble()
      case Type.FIXED32 => in.readFixed32()
      case Type.FIXED64 => in.readFixed64()
      case Type.INT32 => in.readInt32()
      case Type.INT64 => in.readInt64()
      case Type.UINT32 => in.readUInt32()
      case Type.UINT64 => in.readUInt64()
      case Type.SFIXED32 => in.readSFixed32()
      case Type.SFIXED64 => in.readSFixed64()
      case Type.SINT32 => in.readSInt32()
      case Type.SINT64 => in.readSInt64()
      case Type.BOOL => in.readBool()
      case Type.STRING => in.readString()
      case Type.BYTES => BaseEncoding.base64().encode(in.readByteArray())
      case Type.ENUM => schema.enums(field.schema.get).values(in.readEnum())
      case Type.MESSAGE =>
        val nestedIn = CodedInputStream.newInstance(in.readByteBuffer())
        read(nestedIn, schema.messages(field.schema.get))
      case Type.GROUP => throw new IllegalArgumentException("Unsupported type: GROUP")
    }

    val map = Maps.newTreeMap[java.lang.Integer, Any]()
    while (!input.isAtEnd) {
      val tag = input.readTag()
      val id = WireFormat.getTagFieldNumber(tag)
      val field = messageSchema.fields(id)

      if (field.label == Label.REPEATED) {
        if (!map.containsKey(id)) {
          map.put(id, Lists.newArrayList[Any]())
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

    val result = Maps.newLinkedHashMap[String, Any]()
    map.asScala.foreach(kv => result.put(messageSchema.fields(kv._1).name, kv._2))
    result
  }

}
