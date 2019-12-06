protobuf-generic
================

[![Build Status](https://travis-ci.org/nevillelyh/protobuf-generic.svg?branch=master)](https://travis-ci.org/nevillelyh/protobuf-generic)
[![codecov.io](https://codecov.io/github/nevillelyh/protobuf-generic/coverage.svg?branch=master)](https://codecov.io/github/nevillelyh/protobuf-generic?branch=master)
[![GitHub license](https://img.shields.io/github/license/nevillelyh/protobuf-generic.svg)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/me.lyh/protobuf-generic_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/me.lyh/protobuf-generic_2.13)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Manipulate [Protocol Buffers](https://developers.google.com/protocol-buffers/) schemas and records in a generic manner without compiled classes, similar to [Avro](https://avro.apache.org/)'s `GenericRecord`.

# Usage

```scala
import me.lyh.protobuf.generic._

val schema1 = Schema.of[MyRecord]  // generic representation of the protobuf schema
val jsonString = schema1.toJson  // serialize to JSON
val schema2 = Schema.fromJson(jsonString)  // deserialize from JSON

// read protobuf binary without original class
val bytes1: Array[Byte] = // binary MyRecord
val reader = GenericReader.of(schema2)
val record1 = reader.read(bytes1)  // generic record, i.e. Map[String, Any]
val jsonRecord = record1.toJson  // JSON string

// write protobuf binary without orignal class
val record2 = GenericRecord.fromJson(jsonRecord)  // generic record, i.e. Map[String, Any]
val writer = GenericWriter.of(schema2)
val bytes2 = writer.write(record2)  // binary MyRecord
```
# License

Copyright 2016 Neville Li.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
