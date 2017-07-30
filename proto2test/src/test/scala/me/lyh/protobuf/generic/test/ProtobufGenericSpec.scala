package me.lyh.protobuf.generic.test

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

import com.google.protobuf.Message
import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto.Schemas._
import org.scalatest._

import scala.reflect.ClassTag

class ProtobufGenericSpec extends FlatSpec with Matchers {

  def roundTrip[T <: Message : ClassTag](record: T): Unit = {
    val schema = Schema.of[T]
    val schemaCopy = Schema.fromJson(schema.toJson)
    schemaCopy should equal (schema)

    val reader = GenericReader.of(schema)
    val writer = GenericWriter.of(schema)
    val jsonRecord = reader.read(record.toByteArray).toJson
    jsonRecord should equal (reader.read(ByteBuffer.wrap(record.toByteArray)).toJson)
    jsonRecord should equal (reader.read(new ByteArrayInputStream(record.toByteArray)).toJson)
    val bytes = writer.write(GenericRecord.fromJson(jsonRecord))

    val recordCopy = ProtobufType[T].parseFrom(bytes)
    recordCopy should equal (record)
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

}
