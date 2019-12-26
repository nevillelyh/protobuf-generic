package me.lyh.protobuf.generic.jmh

import java.util.concurrent.TimeUnit

import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto3.Schemas
import me.lyh.protobuf.generic.test.Records
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
class ProtoBench {
  private val schema = Schema.of[Schemas.Nested]
  private val jsonSchema = schema.toJson
  private val record = Records.nested
  private val bytes = record.toByteArray

  private val reader = GenericReader.of(schema)
  private val writer = GenericWriter.of(schema)
  private val genericRecord = reader.read(bytes)
  private val jsonRecord = genericRecord.toJson

  private val fieldReader = FieldReader.of(schema, Seq(
    "mixed_field_o.double_field_o",
    "mixed_field_o.string_field_o",
    "mixed_field_o.bytes_field_o",
    "mixed_field_o.color_field_o"
  ))

  @Benchmark def parse: Schema = Schema.of[Schemas.Nested]
  @Benchmark def schemaToJson: String = schema.toJson
  @Benchmark def schemaFromJson: Schema = Schema.fromJson(jsonSchema)

  @Benchmark def read: GenericRecord = reader.read(bytes)
  @Benchmark def write: Array[Byte] = writer.write(genericRecord)

  @Benchmark def recordToJson: String = genericRecord.toJson
  @Benchmark def recordFromJson: GenericRecord = GenericRecord.fromJson(jsonRecord)

  @Benchmark def readFieldsGeneric: Seq[Any] = {
    val r = reader.read(bytes)
    Seq(
      r.get("mixed_field_o").asInstanceOf[GenericRecord].get("double_field_o"),
      r.get("mixed_field_o").asInstanceOf[GenericRecord].get("string_field_o"),
      r.get("mixed_field_o").asInstanceOf[GenericRecord].get("bytes_field_o"),
      r.get("mixed_field_o").asInstanceOf[GenericRecord].get("color_field_o"))
  }

  @Benchmark def readFields: Seq[Any] = fieldReader.read(bytes).toSeq
}
