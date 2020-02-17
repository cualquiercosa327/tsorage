package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.data.ShortDataConverter
import org.scalatest.{FlatSpec, Matchers}

/**
 * https://www.binaryconvert.com
 */
class ShortDataConverterTest extends FlatSpec with Matchers
{
   "A small unsigned short" should "be represented by the right byte array" in {
      ShortDataConverter.fromUnsignedShort(42, true) shouldBe Array[Byte](0x2a, 0)
      ShortDataConverter.fromUnsignedShort(42, false) shouldBe Array[Byte](0, 0x2a)
   }

   "A large unsigned short" should "be represented by the right byte array" in {
      ShortDataConverter.fromUnsignedShort(2000, true) shouldBe Array[Byte](0xd0.toByte, 0x07.toByte)
      ShortDataConverter.fromUnsignedShort(2000, false) shouldBe Array[Byte](0x07.toByte, 0xd0.toByte)
   }

   "A byte array representing a small unsigned short" should "be converted into the right value" in {
      ShortDataConverter.asUnsignedShort(Array(0x2a.toByte, 0x00.toByte), true) shouldBe 42
      ShortDataConverter.asUnsignedShort(Array(0x00.toByte, 0x2a.toByte), false) shouldBe 42
   }

   "A byte array representing a large unsigned short" should "be converted into the right value" in {
      ShortDataConverter.asUnsignedShort(Array(0xd0.toByte, 0x07.toByte), true) shouldBe 2000
      ShortDataConverter.asUnsignedShort(Array(0x07.toByte, 0xd0.toByte), false) shouldBe 2000
   }

   "A small positive signed short" should "be represented by the right byte array" in {
      ShortDataConverter.fromSignedShort(42, true) shouldBe Array[Byte](0x2a.toByte, 0x00.toByte)
      ShortDataConverter.fromSignedShort(42, false) shouldBe Array[Byte](0x00.toByte, 0x2a.toByte)
   }

   "A byte array representing a small positive signed short" should "be converted into the right value" in {
      ShortDataConverter.asSignedShort(Array(0x2a, 0x0), true) shouldBe 42
      ShortDataConverter.asSignedShort(Array(0x0, 0x2a), false) shouldBe 42
   }

   "A small negative signed short" should "be represented by the right byte array" in {
      ShortDataConverter.fromSignedShort(-42, true) shouldBe Array[Byte](0xd6.toByte, 0xff.toByte)
      ShortDataConverter.fromSignedShort(-42, false) shouldBe Array[Byte](0xff.toByte, 0xd6.toByte)
   }

   "A byte array representing a small negative signed short" should "be converted into the right value" in {
      ShortDataConverter.asSignedShort(Array(0xd6.toByte, 0xff.toByte), true) shouldBe -42
      ShortDataConverter.asSignedShort(Array(0xff.toByte, 0xd6.toByte), false) shouldBe -42
   }

   "A large positive signed short" should "be represented by the right byte array" in {
      ShortDataConverter.fromSignedShort(2000, true) shouldBe Array[Byte](0xd0.toByte, 0x07.toByte)
      ShortDataConverter.fromSignedShort(2000, false) shouldBe Array[Byte](0x07.toByte, 0xd0.toByte)
   }

   "A byte array representing a large positive signed short" should "be converted into the right value" in {
      ShortDataConverter.asSignedShort(Array(0xd0.toByte, 0x07.toByte), true) shouldBe 2000
      ShortDataConverter.asSignedShort(Array(0x07.toByte, 0xd0.toByte), false) shouldBe 2000
   }

   "A large negative signed short" should "be represented by the right byte array" in {
      ShortDataConverter.fromSignedShort(-2000, true) shouldBe Array[Byte](0x30.toByte, 0xf8.toByte)
      ShortDataConverter.fromSignedShort(-2000, false) shouldBe Array[Byte](0xf8.toByte, 0x30.toByte)
   }

   "A byte array representing a large negative signed short" should "be converted into the right value" in {
      ShortDataConverter.asSignedShort(Array(0x30.toByte, 0xf8.toByte), true) shouldBe -2000
      ShortDataConverter.asSignedShort(Array(0xf8.toByte, 0x30.toByte), false) shouldBe -2000
   }
}
