package me.lyh.protobuf.generic

import java.io.{ByteArrayOutputStream, OutputStream}

import com.google.common.io.BaseEncoding
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.{CodedOutputStream, WireFormat}
import com.google.protobuf.Descriptors.FieldDescriptor.Type

import scala.collection.JavaConverters._

object GenericWriter {
  def of(schema: Schema): GenericWriter = new GenericWriter(schema)
}

class GenericWriter(val schema: Schema) {

  private val rootSchema = schema.messages(schema.name)

  def write(record: GenericRecord): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    write(record, baos)
    baos.close()
    baos.toByteArray
  }

  def write(record: GenericRecord, output: OutputStream): Unit = {
    val cos = CodedOutputStream.newInstance(output)
    write(record, cos, rootSchema)
    cos.flush()
  }

  private def write(record: GenericRecord,
                    output: CodedOutputStream,
                    messageSchema: MessageSchema): Unit = {
    def writeValue(out: CodedOutputStream, field: Field, value: Any): Unit = field.`type` match {
      case Type.FLOAT => out.writeFloatNoTag(value.toString.toFloat)
      case Type.DOUBLE => out.writeDoubleNoTag(value.toString.toDouble)
      case Type.FIXED32 => out.writeFixed32NoTag(value.toString.toInt)
      case Type.FIXED64 => out.writeFixed64NoTag(value.toString.toLong)
      case Type.INT32 => out.writeInt32NoTag(value.toString.toInt)
      case Type.INT64 => out.writeInt64NoTag(value.toString.toLong)
      case Type.UINT32 => out.writeUInt32NoTag(value.toString.toInt)
      case Type.UINT64 => out.writeUInt64NoTag(value.toString.toLong)
      case Type.SFIXED32 => out.writeSFixed32NoTag(value.toString.toInt)
      case Type.SFIXED64 => out.writeSFixed64NoTag(value.toString.toLong)
      case Type.SINT32 => out.writeSInt32NoTag(value.toString.toInt)
      case Type.SINT64 => out.writeSInt64NoTag(value.toString.toLong)
      case Type.BOOL => out.writeBoolNoTag(value.toString.toBoolean)
      case Type.STRING => out.writeStringNoTag(value.toString)
      case Type.BYTES =>
        out.writeByteArrayNoTag(BaseEncoding.base64().decode(value.toString))
      case Type.ENUM =>
        val enumMap = schema.enums(field.schema.get).values.map(kv => (kv._2, kv._1))
        out.writeEnumNoTag(enumMap(value.toString))
      case Type.MESSAGE =>
        val baos = new ByteArrayOutputStream()
        val bytesOut = CodedOutputStream.newInstance(baos)
        write(value.asInstanceOf[GenericRecord], bytesOut, schema.messages(field.schema.get))
        bytesOut.flush()
        out.writeByteArrayNoTag(baos.toByteArray)
      case Type.GROUP => throw new IllegalArgumentException("Unsupported type: GROUP")
    }

    val fieldMap = messageSchema.fields.map(kv => (kv._2.name, kv._2))
    record.asScala.foreach { case (key, value) =>
      val field = fieldMap(key)
      val wt = wireType(field.`type`)
      if (field.label == Label.REPEATED) {
        val list = value.asInstanceOf[java.util.ArrayList[Any]]
        if (field.packed) {
          val baos = new ByteArrayOutputStream()
          val bytesOut = CodedOutputStream.newInstance(baos)
          list.asScala.foreach(v => writeValue(bytesOut, field, v))
          bytesOut.flush()
          output.writeByteArray(field.id, baos.toByteArray)
        } else {
          list.asScala.foreach { v =>
            output.writeTag(field.id, wt)
            writeValue(output, field, v)
          }
        }
      } else {
        output.writeTag(field.id, wt)
        writeValue(output, field, value)
      }
    }
  }

  private def wireType(fieldType: FieldDescriptor.Type): Int = {
    val t = fieldType match {
      case Type.FLOAT => WireFormat.FieldType.FLOAT
      case Type.DOUBLE => WireFormat.FieldType.DOUBLE
      case Type.FIXED32 => WireFormat.FieldType.FIXED32
      case Type.FIXED64 => WireFormat.FieldType.FIXED64
      case Type.INT32 => WireFormat.FieldType.INT32
      case Type.INT64 => WireFormat.FieldType.INT64
      case Type.UINT32 => WireFormat.FieldType.UINT32
      case Type.UINT64 => WireFormat.FieldType.UINT64
      case Type.SFIXED32 => WireFormat.FieldType.SFIXED32
      case Type.SFIXED64 => WireFormat.FieldType.SFIXED64
      case Type.SINT32 => WireFormat.FieldType.SINT32
      case Type.SINT64 => WireFormat.FieldType.SINT64
      case Type.BOOL => WireFormat.FieldType.BOOL
      case Type.STRING => WireFormat.FieldType.STRING
      case Type.BYTES => WireFormat.FieldType.BYTES
      case Type.ENUM => WireFormat.FieldType.ENUM
      case Type.MESSAGE => WireFormat.FieldType.MESSAGE
      case Type.GROUP => WireFormat.FieldType.GROUP
    }
    t.getWireType
  }

}
