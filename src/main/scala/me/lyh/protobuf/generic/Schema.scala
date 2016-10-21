package me.lyh.protobuf.generic

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label._
import com.google.protobuf.Descriptors.{Descriptor, EnumDescriptor, FieldDescriptor}
import com.google.protobuf.Message

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

case class Schema(name: String,
                  messages: Map[String, MessageSchema],
                  enums: Map[String, EnumSchema])

sealed trait DescriptorSchema
case class MessageSchema(name: String, fields: Map[String, Field]) extends DescriptorSchema
case class EnumSchema(name: String, values: Map[String, String]) extends DescriptorSchema

case class Field(id: Int, name: String,
                 label: Label, `type`: FieldDescriptor.Type, packed: Boolean,
                 schema: Option[String])

object Schema {

  private val schemaMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  def fromJson(json: String): Schema = schemaMapper.readValue(json, classOf[Schema])

  def of[T <: Message : ClassTag]: Schema = {
    val descriptor = ProtobufType[T].descriptor
    val m = toSchemaMap(descriptor)
    val messages = Map.newBuilder[String, MessageSchema]
    val enums = Map.newBuilder[String, EnumSchema]
    m.values.foreach {
      case s: MessageSchema => messages += (s.name -> s)
      case s: EnumSchema => enums += (s.name -> s)
    }
    Schema(descriptor.getFullName, messages.result(), enums.result())
  }

  private def toSchemaMap(descriptor: Descriptor): Map[String, DescriptorSchema] = {
    val (fields, schemas) = descriptor.getFields.asScala
      .foldLeft(Map.empty[String, Field], Map.empty[String, DescriptorSchema]) { (z, fd) =>
        val f = Field(fd.getNumber, fd.getName, getLabel(fd), fd.getType, fd.isPacked, None)
        fd.getType match {
          case FieldDescriptor.Type.MESSAGE =>
            val n = fd.getMessageType.getFullName
            val s = toSchemaMap(fd.getMessageType)
            (z._1 + (f.id.toString -> f.copy(schema = Some(n))), z._2 ++ s)
          case FieldDescriptor.Type.ENUM =>
            val n = fd.getEnumType.getFullName
            val s = toEnumSchema(fd.getEnumType)
            (z._1 + (f.id.toString -> f.copy(schema = Some(n))), z._2 + (s.name -> s))
          case _ =>
            (z._1 + (f.id.toString -> f), z._2)
        }
      }
    schemas + (descriptor.getFullName -> MessageSchema(descriptor.getFullName, fields))
  }

  private def toEnumSchema(ed: EnumDescriptor): EnumSchema = {
    val values = ed.getValues.asScala.map(v => v.getNumber.toString -> v.getName).toMap
    EnumSchema(ed.getFullName, values)
  }

  private def getLabel(fd: FieldDescriptor): Label = fd.toProto.getLabel match {
    case LABEL_REQUIRED => Label.REQUIRED
    case LABEL_OPTIONAL => Label.OPTIONAL
    case LABEL_REPEATED => Label.REPEATED
  }

}
