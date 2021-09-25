package me.lyh.protobuf.generic.jmh

import java.util.concurrent.TimeUnit

import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto2.Schemas
import me.lyh.protobuf.generic.test.Records
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class DefaultBench {
  private val schema = Schema.of[Schemas.CustomDefaults]
  private val record = Schemas.CustomDefaults.getDefaultInstance
  private val bytes = record.toByteArray

  private val reader = GenericReader.of(schema)
  private val fieldReader = FieldReader.of(
    schema,
    Seq(
      "double_field",
      "float_field",
      "int32_field",
      "int64_field",
      "bool_field",
      "string_field",
      "bytes_field",
      "color_field"
    )
  )

  @Benchmark def readFieldsGeneric: Seq[Any] = {
    val r = reader.read(bytes)
    Seq(
      r.get("double_field"),
      r.get("float_field"),
      r.get("int32_field"),
      r.get("int64_field"),
      r.get("bool_field"),
      r.get("string_field"),
      r.get("bytes_field"),
      r.get("color_field")
    )
  }

  @Benchmark def readFields: Seq[Any] = fieldReader.read(bytes).toSeq
}
