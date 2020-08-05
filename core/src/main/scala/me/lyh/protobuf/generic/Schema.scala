package me.lyh.protobuf.generic

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label._
import com.google.protobuf.Descriptors.{Descriptor, EnumDescriptor, FieldDescriptor}
import com.google.protobuf.{ByteString, Descriptors, Message, MessageOrBuilder}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

case class Schema(
  name: String,
  messages: Map[String, MessageSchema],
  enums: Map[String, EnumSchema]
) {
  val root: MessageSchema = messages(name)
}

sealed trait DescriptorSchema

case class MessageSchema(
  name: String,
  fields: Map[Int, Field],
  options: Option[Map[String, String]]
) extends DescriptorSchema

case class EnumSchema(name: String, values: Map[Int, String], options: Option[Map[String, String]])
    extends DescriptorSchema

case class Field(
  id: Int,
  name: String,
  label: Label,
  `type`: FieldDescriptor.Type,
  packed: Boolean,
  default: Option[AnyRef],
  schema: Option[String],
  options: Option[Map[String, String]]
)

object Schema {
  def fromJson(json: String): Schema = SchemaMapper.fromJson(json)

  def of[T <: Message: ClassTag]: Schema = {
    val descriptor = ProtobufType[T].descriptor
    val m = toSchemaMap(descriptor, Set(descriptor.getFullName))
    val messages = Map.newBuilder[String, MessageSchema]
    val enums = Map.newBuilder[String, EnumSchema]
    m.values.foreach {
      case s: MessageSchema => messages += (s.name -> s)
      case s: EnumSchema    => enums += (s.name -> s)
    }
    Schema(descriptor.getFullName, messages.result(), enums.result())
  }

  private def toSchemaMap(
    descriptor: Descriptor,
    seen: Set[String]
  ): Map[String, DescriptorSchema] = {
    val (fields, schemas) = descriptor.getFields.asScala
      .foldLeft(Map.empty[Int, Field], Map.empty[String, DescriptorSchema]) { (z, fd) =>
        val default = if (fd.hasDefaultValue) {
          val v = fd.getDefaultValue
          val json = fd.getType match {
            case FieldDescriptor.Type.ENUM =>
              v.asInstanceOf[Descriptors.EnumValueDescriptor].getName
            case FieldDescriptor.Type.BYTES =>
              Base64.encode(v.asInstanceOf[ByteString].toByteArray)
            case _ => v
          }
          Some(json)
        } else {
          None
        }
        val fieldOpts = optionMap(fd.getOptions)
        val f =
          Field(
            fd.getNumber,
            fd.getName,
            getLabel(fd),
            fd.getType,
            fd.isPacked,
            default,
            None,
            fieldOpts
          )
        fd.getType match {
          case FieldDescriptor.Type.MESSAGE =>
            val n = fd.getMessageType.getFullName
            val s = if (!seen(n)) toSchemaMap(fd.getMessageType, seen + n) else Map.empty
            (z._1 + (f.id -> f.copy(schema = Some(n))), z._2 ++ s)
          case FieldDescriptor.Type.ENUM =>
            val n = fd.getEnumType.getFullName
            val s = toEnumSchema(fd.getEnumType)
            (z._1 + (f.id -> f.copy(schema = Some(n))), z._2 + (s.name -> s))
          case _ =>
            (z._1 + (f.id -> f), z._2)
        }
      }
    val msgOpts = optionMap(descriptor.getOptions)
    schemas + (descriptor.getFullName -> MessageSchema(descriptor.getFullName, fields, msgOpts))
  }

  private def toEnumSchema(ed: EnumDescriptor): EnumSchema = {
    val values = ed.getValues.asScala.map(v => v.getNumber -> v.getName).toMap
    EnumSchema(ed.getFullName, values, optionMap(ed.getOptions))
  }

  private def getLabel(fd: FieldDescriptor): Label = fd.toProto.getLabel match {
    case LABEL_REQUIRED => Label.REQUIRED
    case LABEL_OPTIONAL => Label.OPTIONAL
    case LABEL_REPEATED => Label.REPEATED
  }

  private[generic] def optionMap(options: MessageOrBuilder): Option[Map[String, String]] = {
    val optionsMap = options.getAllFields.asScala.foldLeft(Map.empty[String, String]) {
      case (map, (desc, ref)) => map + (desc.getFullName -> ref.toString)
    }
    if (optionsMap.nonEmpty) Some(optionsMap) else None
  }
}

private[generic] object SchemaMapper {
  private val schemaMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(DefaultScalaModule)

  case class JSchema(name: String, messages: Iterable[JMessageSchema], enums: Iterable[JEnumSchema])

  case class JMessageSchema(
    name: String,
    fields: Iterable[Field],
    options: Option[Map[String, String]]
  )

  case class JEnumSchema(
    name: String,
    values: Map[String, Int],
    options: Option[Map[String, String]]
  )

  def toJson(schema: Schema): String = {
    val jSchema = JSchema(
      schema.name,
      schema.messages.values.map(toJMessageSchema),
      schema.enums.values.map(toJEnumSchema)
    )
    schemaMapper.writeValueAsString(jSchema)
  }

  def fromJson(json: String): Schema = {
    val schema = schemaMapper.readValue(json, classOf[JSchema])
    Schema(
      schema.name,
      schema.messages.map(m => m.name -> fromJMessageSchema(m)).toMap,
      schema.enums.map(e => e.name -> fromJEnumSchema(e)).toMap
    )
  }

  private def fromJMessageSchema(schema: JMessageSchema): MessageSchema =
    MessageSchema(schema.name, schema.fields.map(f => f.id -> f).toMap, schema.options)

  private def toJMessageSchema(schema: MessageSchema): JMessageSchema =
    JMessageSchema(schema.name, schema.fields.values.toList.sortBy(_.id), schema.options)

  private def fromJEnumSchema(schema: JEnumSchema): EnumSchema =
    EnumSchema(schema.name, schema.values.map(kv => kv._2 -> kv._1), schema.options)

  private def toJEnumSchema(schema: EnumSchema): JEnumSchema = {
    val m = Map.newBuilder[String, Int]
    m ++= schema.values.toList.sortBy(_._1).map(kv => kv._2 -> kv._1)
    JEnumSchema(schema.name, m.result(), schema.options)
  }
}
