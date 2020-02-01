package be.cetic.tsorage.collector.modbus

import org.scalatest.{FlatSpec, Matchers}

/**
 * https://www.binaryconvert.com
 */
class ByteDataconverterTest  extends FlatSpec with Matchers
{
   "A small unsigned byte" should "be represented by the right byte array" in {
      ByteDataConverter.fromUnsignedByte(42, true) shouldBe Array[Byte](0x2a.toByte)
      ByteDataConverter.fromUnsignedByte(42, false) shouldBe Array[Byte](0x2a.toByte)
   }

   "A large unsigned byte" should "be represented by the right byte array" in {
      ByteDataConverter.fromUnsignedByte(200, true) shouldBe Array[Byte](0xc8.toByte)
      ByteDataConverter.fromUnsignedByte(200, false) shouldBe Array[Byte](0xc8.toByte)
   }

   "A byte array representing a small unsigned byte" should "be converted into the right value" in {
      ByteDataConverter.asUnsignedByte(Array(0x2a.toByte), true) shouldBe 42
      ByteDataConverter.asUnsignedByte(Array(0x2a.toByte), false) shouldBe 42
   }

   "A byte array representing a large unsigned byte" should "be converted into the right value" in {
      ByteDataConverter.asUnsignedByte(Array(0xc8.toByte), true) shouldBe 200
      ByteDataConverter.asUnsignedByte(Array(0xc8.toByte), false) shouldBe 200
   }

   "A small positive signed byte" should "be represented by the right byte array" in {
      ByteDataConverter.fromSignedByte(42, true) shouldBe Array[Byte](0x2a)
      ByteDataConverter.fromSignedByte(42, false) shouldBe Array[Byte](0x2a)
   }

   "A byte array representing a small positive signed byte" should "be converted into the right value" in {
      ByteDataConverter.asSignedByte(Array(0x2a), true) shouldBe 42
      ByteDataConverter.asSignedByte(Array(0x2a), false) shouldBe 42
   }

   "A small negative signed byte" should "be represented by the right byte array" in {
      ByteDataConverter.fromSignedByte(-42, true) shouldBe Array[Byte](0xd6.toByte)
      ByteDataConverter.fromSignedByte(-42, false) shouldBe Array[Byte](0xd6.toByte)
   }

   "A byte array representing a small negative signed byte" should "be converted into the right value" in {
      ByteDataConverter.asSignedByte(Array(0xd6.toByte), true) shouldBe -42
      ByteDataConverter.asSignedByte(Array(0xd6.toByte), false) shouldBe -42
   }

   "A large positive signed byte" should "be represented by the right byte array" in {
      ByteDataConverter.fromSignedByte(123, true) shouldBe Array[Byte](0x7b.toByte)
      ByteDataConverter.fromSignedByte(123, false) shouldBe Array[Byte](0x7b.toByte)
   }

   "A byte array representing a large positive signed byte" should "be converted into the right value" in {
      ByteDataConverter.asSignedByte(Array(0x7b.toByte), true) shouldBe 123
      ByteDataConverter.asSignedByte(Array(0x7b.toByte), false) shouldBe 123
   }

   "A large negative signed byte" should "be represented by the right byte array" in {
      ByteDataConverter.fromSignedByte(-123, true) shouldBe Array[Byte](0x85.toByte)
      ByteDataConverter.fromSignedByte(-123, false) shouldBe Array[Byte](0x85.toByte)
   }

   "A byte array representing a large negative signed byte" should "be converted into the right value" in {
      ByteDataConverter.asSignedByte(Array(0x85.toByte), true) shouldBe -123
      ByteDataConverter.asSignedByte(Array(0x85.toByte), false) shouldBe -123
   }
}
