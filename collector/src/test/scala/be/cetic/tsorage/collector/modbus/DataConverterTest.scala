package be.cetic.tsorage.collector.modbus

import org.scalatest.{FlatSpec, Matchers}

class DataConverterTest extends FlatSpec with Matchers
{
   "A small unsigned byte" should "be converted into a one-byte byte array" in {
      DataConverter.fromUnsignedByte(42).size shouldBe 1
   }

   it should "be represented by the right byte array" in {
      DataConverter.fromUnsignedByte(42) shouldBe Array[Byte](42)
      DataConverter.fromUnsignedByte(42).map(_.toHexString.takeRight(2)).mkString("") shouldBe "2a"
   }

   "A large unsigned byte" should "be converted into a one-byte byte array for large values" in {
      DataConverter.fromUnsignedByte(200).size shouldBe 1
   }

  it should "be represented by the right byte array" in {
      DataConverter.fromUnsignedByte(200) shouldBe Array[Byte](200.toByte)
      DataConverter.fromUnsignedByte(200).map(_.toHexString.takeRight(2)).mkString("") shouldBe "c8"
   }
}
