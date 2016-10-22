package me.lyh.protobuf.generic.test

import java.io.ByteArrayInputStream

import com.google.protobuf.CodedInputStream
import me.lyh.protobuf.generic._
import me.lyh.protobuf.generic.proto.Schemas._
import org.scalatest._

class ProtobufTypeSpec extends FlatSpec with Matchers {

  val pt = ProtobufType[Required]
  val record = Records.required

  "ProtobufType.descriptor" should "work" in {
    pt.descriptor should equal (Required.getDescriptor)
  }

  "ProtobufType.parseFrom" should "support byte array" in {
    pt.parseFrom(record.toByteArray) should equal (record)
  }

  it should "support ByteString" in {
    pt.parseFrom(record.toByteString) should equal (record)
  }

  it should "support InputStream" in {
    pt.parseFrom(new ByteArrayInputStream(record.toByteArray)) should equal (record)
  }

  it should "support CodedInputStream" in {
    pt.parseFrom(CodedInputStream.newInstance(record.toByteArray)) should equal (record)
  }

}
