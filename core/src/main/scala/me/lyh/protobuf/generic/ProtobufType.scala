package me.lyh.protobuf.generic

import java.io.InputStream

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.{ByteString, CodedInputStream, Message}

import scala.reflect.ClassTag

object ProtobufType {
  def apply[T <: Message: ClassTag]: ProtobufType[T] = new ProtobufType[T]
}

class ProtobufType[T <: Message: ClassTag] {
  private val cls = implicitly[ClassTag[T]].runtimeClass

  def descriptor: Descriptor = cls.getMethod("getDescriptor").invoke(null).asInstanceOf[Descriptor]

  def newBuilder(): Message.Builder =
    cls.getMethod("newBuilder").invoke(null).asInstanceOf[Message.Builder]

  def parseFrom(data: Array[Byte]): T =
    cls.getMethod("parseFrom", classOf[Array[Byte]]).invoke(null, data).asInstanceOf[T]

  def parseFrom(data: ByteString): T =
    cls.getMethod("parseFrom", classOf[ByteString]).invoke(null, data).asInstanceOf[T]

  def parseFrom(input: InputStream): T =
    cls.getMethod("parseFrom", classOf[InputStream]).invoke(null, input).asInstanceOf[T]

  def parseFrom(input: CodedInputStream): T =
    cls.getMethod("parseFrom", classOf[CodedInputStream]).invoke(null, input).asInstanceOf[T]
}
