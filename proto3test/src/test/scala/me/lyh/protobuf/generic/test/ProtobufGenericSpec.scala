package me.lyh.protobuf.generic.test

import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto.Schemas._
import org.scalatest._

import scala.reflect.ClassTag

class ProtobufGenericSpec extends FlatSpec with Matchers {

  private val printer = JsonFormat.printer().preservingProtoFieldNames()
  private val parser = JsonFormat.parser()

  def roundTrip[T <: Message : ClassTag](record: T): Unit = {
    val schema = Schema.of[T]
    val schemaCopy = Schema.fromJson(schema.toJson)
    schemaCopy shouldBe schema

    val reader = GenericReader.of(schema)
    val writer = GenericWriter.of(schema)
    val jsonRecord = reader.read(record.toByteArray).toJson
    val bytes = writer.write(GenericRecord.fromJson(jsonRecord))
    val recordCopy = ProtobufType[T].parseFrom(bytes)
    recordCopy shouldBe record

    compatibleWithJsonFormat(record)
  }

  def compatibleWithJsonFormat[T <: Message : ClassTag](record: T): Unit = {
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

  def test[T <: Message : ClassTag](record: T): Unit = {
    roundTrip(record)
    compatibleWithJsonFormat(record)
  }

  "ProtobufGeneric" should "round trip optional" in {
    test[Optional](Records.optional)
    test[Optional](Records.optionalEmpty)
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

}
