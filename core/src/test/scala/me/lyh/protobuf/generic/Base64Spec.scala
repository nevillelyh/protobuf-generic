package me.lyh.protobuf.generic

import org.scalatest._

import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Base64Spec extends AnyFlatSpec with Matchers {
  private def nextBytes: Array[Byte] = {
    val bytes = new Array[Byte](Random.nextInt(100) + 1)
    Random.nextBytes(bytes)
    bytes
  }

  "Base64" should "round trip bytes" in {
    val data = Seq.fill(100)(nextBytes)
    data.map(d => Base64.decode(Base64.encode(d)).toSeq) shouldBe data.map(_.toSeq)
  }
}
