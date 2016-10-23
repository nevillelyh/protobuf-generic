package me.lyh.protobuf

import com.fasterxml.jackson.databind.ObjectMapper

package object generic {

  type GenericRecord = java.util.Map[String, Any]

  private val recordMapper = new ObjectMapper()

  implicit class JsonGenericRecord(val record: GenericRecord) {
    def toJson: String = recordMapper.writeValueAsString(record)
  }

  object GenericRecord {
    def fromJson(json: String): GenericRecord = recordMapper.readValue(json, classOf[GenericRecord])
  }

  implicit class JsonSchema(val schema: Schema) {
    def toJson: String = SchemaMapper.toJson(schema)
  }

}
