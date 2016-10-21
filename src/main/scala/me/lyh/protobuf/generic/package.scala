package me.lyh.protobuf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

package object generic {

  type GenericRecord = java.util.Map[String, Any]

  private val schemaMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val recordMapper = new ObjectMapper()

  implicit class JsonSchema(val schema: Schema) {
    def toJson: String = schemaMapper.writeValueAsString(schema)
  }

  implicit class JsonGenericRecord(val record: GenericRecord) {
    def toJson: String = recordMapper.writeValueAsString(record)
  }

  object GenericRecord {
    def fromJson(json: String): GenericRecord = recordMapper.readValue(json, classOf[GenericRecord])
  }

}
