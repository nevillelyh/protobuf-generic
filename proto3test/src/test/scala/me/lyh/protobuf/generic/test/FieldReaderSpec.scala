package me.lyh.protobuf.generic.test

import com.google.protobuf.{ByteString, Message}
import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto.Schemas._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.reflect.ClassTag

class FieldReaderSpec extends AnyFlatSpec with Matchers {
  def read[T <: Message: ClassTag](record: T, fields: List[String], expected: List[Any]): Unit = {
    val schema = Schema.of[T]
    val reader = FieldReader.of(schema, fields)
    val actual = reader.read(record.toByteArray)
    actual.toList shouldBe expected
  }

  val fields: List[String] = List(
    "double_field",
    "float_field",
    "int32_field",
    "int64_field",
    "uint32_field",
    "uint64_field",
    "sint32_field",
    "sint64_field",
    "fixed32_field",
    "fixed64_field",
    "sfixed32_field",
    "sfixed64_field",
    "bool_field",
    "string_field",
    "bytes_field",
    "color_field"
  )

  val expected: List[Any] = List(
    math.Pi,
    math.E.toFloat,
    10,
    15,
    20,
    25,
    30,
    35,
    40,
    45,
    50,
    55,
    true,
    "hello",
    ByteString.copyFromUtf8("world"),
    "WHITE"
  )

  "FieldReader" should "read optional" in {
    read[Optional](Records.optional, fields, expected)
    read[Optional](
      Records.optionalEmpty,
      fields,
      List(0.0, 0.0f, 0, 0L, 0, 0L, 0, 0L, 0, 0L, 0, 0L, false, "", ByteString.EMPTY, "BLACK")
    )
  }

  it should "read oneofs" in {
    (Records.oneOfs.drop(1) zip (fields zip expected)).foreach {
      case (r, (f, e)) =>
        read[OneOf](r, List(f), List(e))
    }
  }

  it should "read nested" in {
    val fields = List(
      "mixed_field_o.double_field_o",
      "mixed_field_o.string_field_o",
      "mixed_field_o.bytes_field_o",
      "mixed_field_o.color_field_o"
    )
    val expected = List(math.Pi, "hello", ByteString.copyFromUtf8("world"), "WHITE")
    read[Nested](Records.nested, fields, expected)

    val expectedEmpty = List(0.0, "", ByteString.EMPTY, "BLACK")
    read[Nested](Records.nestedEmpty, fields, expectedEmpty)
  }
}
