package me.lyh.protobuf.generic.test

import com.google.protobuf.ByteString
import me.lyh.protobuf.generic.proto.Schemas._

import scala.collection.JavaConverters._

object Records {

  def jList[T](xs: T*): java.util.List[T] = xs.asJava

  val required = Required.newBuilder()
    .setDoubleField(math.Pi)
    .setFloatField(math.E.toFloat)
    .setInt32Field(10)
    .setInt64Field(15)
    .setUint32Field(20)
    .setUint64Field(25)
    .setSint32Field(30)
    .setSint64Field(35)
    .setFixed32Field(40)
    .setFixed64Field(45)
    .setSfixed32Field(50)
    .setSfixed64Field(55)
    .setBoolField(true)
    .setStringField("hello")
    .setBytesField(ByteString.copyFromUtf8("world"))
    .setColorField(Color.WHITE)
    .build()

  val optional = Optional.newBuilder()
    .setDoubleField(math.Pi)
    .setFloatField(math.E.toFloat)
    .setInt32Field(10)
    .setInt64Field(15)
    .setUint32Field(20)
    .setUint64Field(25)
    .setSint32Field(30)
    .setSint64Field(35)
    .setFixed32Field(40)
    .setFixed64Field(45)
    .setSfixed32Field(50)
    .setSfixed64Field(55)
    .setBoolField(true)
    .setStringField("hello")
    .setBytesField(ByteString.copyFromUtf8("world"))
    .setColorField(Color.BLACK)
    .build()

  val optionalEmpty = Optional.getDefaultInstance

  val repeated = Repeated.newBuilder()
    .addAllDoubleField(jList(math.Pi, -math.Pi))
    .addAllFloatField(jList(math.E.toFloat, -math.E.toFloat))
    .addAllInt32Field(jList(10, 11))
    .addAllInt64Field(jList(15L, 16L))
    .addAllUint32Field(jList(20, 21))
    .addAllUint64Field(jList(25L, 26L))
    .addAllSint32Field(jList(30, 31))
    .addAllSint64Field(jList(35L, 36L))
    .addAllFixed32Field(jList(40, 41))
    .addAllFixed64Field(jList(45L, 46L))
    .addAllSfixed32Field(jList(50, 51))
    .addAllSfixed64Field(jList(55L, 56L))
    .addAllBoolField(jList(true, false))
    .addAllStringField(jList("hello", "world"))
    .addAllBytesField(jList(ByteString.copyFromUtf8("hello"), ByteString.copyFromUtf8("world")))
    .addAllColorField(jList(Color.BLACK, Color.WHITE))
    .build()

  val repeatedEmpty = Repeated.getDefaultInstance

  val repeatedPacked = RepeatedPacked.newBuilder()
    .addAllDoubleField(jList(math.Pi, -math.Pi))
    .addAllFloatField(jList(math.E.toFloat, -math.E.toFloat))
    .addAllInt32Field(jList(10, 11))
    .addAllInt64Field(jList(15L, 16L))
    .addAllUint32Field(jList(20, 21))
    .addAllUint64Field(jList(25L, 26L))
    .addAllSint32Field(jList(30, 31))
    .addAllSint64Field(jList(35L, 36L))
    .addAllFixed32Field(jList(40, 41))
    .addAllFixed64Field(jList(45L, 46L))
    .addAllSfixed32Field(jList(50, 51))
    .addAllSfixed64Field(jList(55L, 36L))
    .addAllBoolField(jList(true, false))
    .addAllColorField(jList(Color.BLACK, Color.WHITE))
    .build()

  val repeatedUnpacked = RepeatedUnpacked.newBuilder()
    .addAllDoubleField(jList(math.Pi, -math.Pi))
    .addAllFloatField(jList(math.E.toFloat, -math.E.toFloat))
    .addAllInt32Field(jList(10, 11))
    .addAllInt64Field(jList(15L, 16L))
    .addAllUint32Field(jList(20, 21))
    .addAllUint64Field(jList(25L, 26L))
    .addAllSint32Field(jList(30, 31))
    .addAllSint64Field(jList(35L, 36L))
    .addAllFixed32Field(jList(40, 41))
    .addAllFixed64Field(jList(45L, 46L))
    .addAllSfixed32Field(jList(50, 51))
    .addAllSfixed64Field(jList(55L, 36L))
    .addAllBoolField(jList(true, false))
    .addAllColorField(jList(Color.BLACK, Color.WHITE))
    .build()

  val oneOfs = Array(
    OneOf.getDefaultInstance,
    OneOf.newBuilder().setDoubleField(math.Pi).build(),
    OneOf.newBuilder().setFloatField(math.E.toFloat).build(),
    OneOf.newBuilder().setInt32Field(10).build(),
    OneOf.newBuilder().setInt64Field(15L).build(),
    OneOf.newBuilder().setUint32Field(20).build(),
    OneOf.newBuilder().setUint64Field(25L).build(),
    OneOf.newBuilder().setSint32Field(30).build(),
    OneOf.newBuilder().setSint64Field(35L).build(),
    OneOf.newBuilder().setFixed32Field(40).build(),
    OneOf.newBuilder().setFixed64Field(45L).build(),
    OneOf.newBuilder().setSfixed32Field(50).build(),
    OneOf.newBuilder().setSfixed64Field(55L).build(),
    OneOf.newBuilder().setBoolField(true).build(),
    OneOf.newBuilder().setStringField("hello").build(),
    OneOf.newBuilder().setBytesField(ByteString.copyFromUtf8("world")).build(),
    OneOf.newBuilder().setColorField(Color.BLACK).build())

  val mixed = Mixed.newBuilder()
    .setDoubleField(math.Pi)
    .setStringField("hello")
    .setBytesField(ByteString.copyFromUtf8("world"))
    .setColorField(Color.BLACK)
    .setDoubleFieldO(math.Pi)
    .setStringFieldO("hello")
    .setBytesFieldO(ByteString.copyFromUtf8("world"))
    .setColorFieldO(Color.BLACK)
    .addAllDoubleFieldR(jList(math.Pi, -math.Pi))
    .addAllStringFieldR(jList("hello", "world"))
    .addAllBytesFieldR(jList(ByteString.copyFromUtf8("hello"), ByteString.copyFromUtf8("world")))
    .addAllColorFieldR(jList(Color.BLACK, Color.WHITE))
    .build()

  val mixedEmpty = Mixed.newBuilder()
    .setDoubleField(math.Pi)
    .setStringField("hello")
    .setBytesField(ByteString.copyFromUtf8("world"))
    .setColorField(Color.BLACK)
    .build()

  val nested = Nested.newBuilder()
    .setDoubleField(math.Pi)
    .setDoubleFieldO(math.Pi)
    .addAllDoubleFieldR(jList(math.Pi, -math.Pi))
    .setColorField(Color.BLACK)
    .setColorFieldO(Color.BLACK)
    .addAllColorFieldR(jList(Color.BLACK, Color.WHITE))
    .setMixedField(mixed)
    .setMixedFieldO(mixed)
    .addAllMixedFieldR(jList(mixed, mixedEmpty))
    .build()

  val nestedEmpty = Nested.newBuilder()
    .setDoubleField(math.Pi)
    .setColorField(Color.BLACK)
    .setMixedField(mixed)
    .build()

}
