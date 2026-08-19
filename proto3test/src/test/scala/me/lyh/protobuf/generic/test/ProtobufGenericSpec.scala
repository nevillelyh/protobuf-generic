package me.lyh.protobuf.generic.test

import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto3.Schemas._

import scala.reflect.ClassTag
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProtobufGenericSpec extends AnyFlatSpec with Matchers {
  private val printer = JsonFormat.printer().preservingProtoFieldNames()
  private val parser = JsonFormat.parser()

  def roundTrip[T <: Message: ClassTag](record: T): Unit = {
    val schema = SerializableUtils.ensureSerializable(Schema.of[T])
    val schemaCopy = Schema.fromJson(schema.toJson)
    schemaCopy shouldBe schema

    val reader = SerializableUtils.ensureSerializable(GenericReader.of(schema))
    val writer = SerializableUtils.ensureSerializable(GenericWriter.of(schema))
    val jsonRecord = reader.read(record.toByteArray).toJson
    val bytes = writer.write(GenericRecord.fromJson(jsonRecord))
    val recordCopy = ProtobufType[T].parseFrom(bytes)
    recordCopy shouldBe record

    compatibleWithJsonFormat(record)
  }

  def compatibleWithJsonFormat[T <: Message: ClassTag](record: T): Unit = {
    val protoType = ProtobufType[T]
    val schema = Schema.of[T]
    val reader = GenericReader.of(schema)
    val writer = GenericWriter.of(schema)

    val json1 = reader.read(record.toByteArray).toJson
    val record1 = {
      val builder = protoType.newBuilder()
      parser.merge(json1, builder)
      builder.build().asInstanceOf[T]
    }

    val json2 = printer.print(record)
    val record2 = protoType.parseFrom(writer.write(GenericRecord.fromJson(json2)))

    record1 shouldBe record2
  }

  def test[T <: Message: ClassTag](record: T): Unit = {
    roundTrip(record)
    compatibleWithJsonFormat(record)
  }

  "ProtobufGeneric" should "round trip optional" in {
    test[Optional](Records.optional)
    test[Optional](Records.optionalEmpty)
  }

  it should "write explicit presence" in {
    val writer = GenericWriter.of(Schema.fromJson(Schema.of[Presence].toJson))
    val record = new java.util.HashMap[String, Any]()

    Presence.parseFrom(writer.write(record)).hasExplicitField shouldBe false

    record.put("explicit_field", 0)
    Presence.parseFrom(writer.write(record)).hasExplicitField shouldBe true

    record.put("explicit_field", None)
    Presence.parseFrom(writer.write(record)).hasExplicitField shouldBe false

    record.put("explicit_field", Some(0))
    Presence.parseFrom(writer.write(record)).hasExplicitField shouldBe true

    record.clear()
    record.put("message_field", new java.util.HashMap[String, Any]())
    Presence.parseFrom(writer.write(record)).hasMessageField shouldBe true
  }

  it should "round trip repeated" in {
    test[Repeated](Records.repeated)
    test[Repeated](Records.repeatedEmpty)
    test[RepeatedPacked](Records.repeatedPacked)
    test[RepeatedUnpacked](Records.repeatedUnpacked)
  }

  it should "round trip oneofs" in {
    Records.oneOfs.foreach(test[OneOf])
  }

  it should "round trip mixed" in {
    test[Mixed](Records.mixed)
    test[Mixed](Records.mixedEmpty)
  }

  it should "round trip nested" in {
    test[Nested](Records.nested)
    test[Nested](Records.nestedEmpty)
  }

  it should "round trip with custom options" in {
    test[CustomOptionMessage](Records.customOptionMessage)
    test[CustomOptionMessage](Records.customOptionMessageEmpty)
  }

  it should "round trip recursive" in {
    test[Recursive1](Records.recursive1)
    test[Recursive2](Records.recursive2)
  }
}
