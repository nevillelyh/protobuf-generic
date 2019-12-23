package me.lyh.protobuf.generic.test

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

import com.google.protobuf.{ByteString, Message}
import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto.Schemas._

import scala.reflect.ClassTag
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProtobufGenericSpec extends AnyFlatSpec with Matchers {
  def roundTrip[T <: Message: ClassTag](record: T): Unit = {
    val schema = SerializableUtils.ensureSerializable(Schema.of[T])
    val schemaCopy = Schema.fromJson(schema.toJson)
    schemaCopy shouldBe schema

    val reader = SerializableUtils.ensureSerializable(GenericReader.of(schema))
    val writer = SerializableUtils.ensureSerializable(GenericWriter.of(schema))
    val jsonRecord = reader.read(record.toByteArray).toJson
    jsonRecord shouldBe reader.read(ByteBuffer.wrap(record.toByteArray)).toJson
    jsonRecord shouldBe reader.read(new ByteArrayInputStream(record.toByteArray)).toJson
    val bytes = writer.write(GenericRecord.fromJson(jsonRecord))

    val recordCopy = ProtobufType[T].parseFrom(bytes)
    recordCopy shouldBe record
  }

  "ProtobufGeneric" should "round trip required" in {
    roundTrip[Required](Records.required)
  }

  it should "round trip optional" in {
    roundTrip[Optional](Records.optional)
    roundTrip[Optional](Records.optionalEmpty)
  }

  it should "round trip repeated" in {
    roundTrip[Repeated](Records.repeated)
    roundTrip[Repeated](Records.repeatedEmpty)
    roundTrip[RepeatedPacked](Records.repeatedPacked)
    roundTrip[RepeatedUnpacked](Records.repeatedUnpacked)
  }

  it should "round trip oneofs" in {
    Records.oneOfs.foreach(roundTrip[OneOf])
  }

  it should "round trip mixed" in {
    roundTrip[Mixed](Records.mixed)
    roundTrip[Mixed](Records.mixedEmpty)
  }

  it should "round trip nested" in {
    roundTrip[Nested](Records.nested)
    roundTrip[Nested](Records.nestedEmpty)
  }

  it should "round trip with custom options" in {
    roundTrip[CustomOptionMessage](Records.customOptionMessage)
    roundTrip[CustomOptionMessage](Records.customOptionMessageEmpty)
  }

  it should "round trip with custom defaults" in {
    roundTrip[CustomDefaults](CustomDefaults.getDefaultInstance)
  }

  it should "populate default values" in {
    val schema = Schema.of[CustomDefaults]
    val record = GenericReader.of(schema).read(CustomDefaults.getDefaultInstance.toByteArray)
    record.get("double_field") shouldBe 101.0
    record.get("float_field") shouldBe 102.0f
    record.get("int32_field") shouldBe 103
    record.get("int64_field") shouldBe 104L
    record.get("uint32_field") shouldBe 105
    record.get("uint64_field") shouldBe 106L
    record.get("sint32_field") shouldBe 107
    record.get("sint64_field") shouldBe 108L
    record.get("fixed32_field") shouldBe 109
    record.get("fixed64_field") shouldBe 110L
    record.get("sfixed32_field") shouldBe 111
    record.get("sfixed64_field") shouldBe 112L
    record.get("bool_field") shouldBe true
    record.get("string_field") shouldBe "hello"
    record.get("bytes_field") shouldBe
      Base64.encode(ByteString.copyFromUtf8("world").toByteArray)
    record.get("color_field") shouldBe "GREEN"
  }
}
