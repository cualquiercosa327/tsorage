package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.data.ByteDataConverter
import org.scalatest.{FlatSpec, Matchers, WordSpec}

/**
 * https://www.binaryconvert.com
 */
class ByteDataconverterTest extends WordSpec with Matchers
{
   "A byte value" when {
      "being a small unsigned byte" should {
         "be converted to the right byte array" in {
            ByteDataConverter.fromUnsignedByte(42) shouldBe Array[Byte](0x2a.toByte)
         }
      }

      "being a large unsigned byte" should {
         "be converted to the right byte array" in {
            ByteDataConverter.fromUnsignedByte(200) shouldBe Array[Byte](0xc8.toByte)
         }
      }

      "being a small signed positive value" should {
         "be converted to the right little endian byte array" in {
            ByteDataConverter.fromSignedByte(42, true) shouldBe Array[Byte](0x2a)
         }

         "be converted to the right little big byte array" in {
            ByteDataConverter.fromSignedByte(42, false) shouldBe Array[Byte](0x2a)
         }
      }

      "being a small signed negative value" should {
         "be converted to the right little endian byte array" in {
            ByteDataConverter.fromSignedByte(-42, true) shouldBe Array[Byte](0xd6.toByte)
         }

         "be converted to the right little big byte array" in {
            ByteDataConverter.fromSignedByte(-42, false) shouldBe Array[Byte](0xd6.toByte)
         }
      }

      "being a large signed positive value" should {
         "be converted to the right little endian byte array" in {
            ByteDataConverter.fromSignedByte(123, true) shouldBe Array[Byte](0x7b.toByte)
         }

         "be converted to the right little big byte array" in {
            ByteDataConverter.fromSignedByte(123, false) shouldBe Array[Byte](0x7b.toByte)
         }
      }

      "being a large signed negative value" should {
         "be converted to the right little endian byte array" in {
            ByteDataConverter.fromSignedByte(-123, true) shouldBe Array[Byte](0x85.toByte)
         }

         "be converted to the right little big byte array" in {
            ByteDataConverter.fromSignedByte(-123, false) shouldBe Array[Byte](0x85.toByte)
         }
      }
   }

   "A byte array" when {
      "representing a small signed positive byte with little endian convention" should {
         "be converted to the right value" in {
            ByteDataConverter.asSignedByte(Array(0x2a), true) shouldBe 42
         }
      }

      "representing a small signed positive byte with big endian convention" should {
         "be converted to the right value" in {
            ByteDataConverter.asSignedByte(Array(0x2a), false) shouldBe 42
         }
      }

      "representing a small signed negative byte with little endian convention" should {
         "be converted to the right value" in {
            ByteDataConverter.asSignedByte(Array(0xd6.toByte), true) shouldBe -42
         }
      }

      "representing a small signed negative byte with big endian convention" should {
         "be converted to the right value" in {
            ByteDataConverter.asSignedByte(Array(0xd6.toByte), false) shouldBe -42
         }
      }

      "representing a small unsigned byte" should {
         "be converted to the right value with little endian convention" in {
            ByteDataConverter.asUnsignedByte(Array(0x2a.toByte), true) shouldBe 42
         }

         "be converted to the right value with big endian convention" in {
            ByteDataConverter.asUnsignedByte(Array(0x2a.toByte), false) shouldBe 42
         }
      }

      "representing a large unsigned byte" should {
         "be converted to the right value with little endian convention" in {
            ByteDataConverter.asUnsignedByte(Array(0xc8.toByte), true) shouldBe 200
         }

         "be converted to the right value with big endian convention" in {
            ByteDataConverter.asUnsignedByte(Array(0xc8.toByte), false) shouldBe 200
         }
      }

      "representing a large signed positive byte" should {
         "be converted to the right value with little endian convention" in {
            ByteDataConverter.asSignedByte(Array(0x7b.toByte), true) shouldBe 123
         }

         "be converted to the right value with big endian convention" in {
            ByteDataConverter.asSignedByte(Array(0x7b.toByte), false) shouldBe 123
         }
      }

      "representing a large signed negative byte" should {
         "be converted to the right value with little endian convention" in {
            ByteDataConverter.asSignedByte(Array(0x85.toByte), true) shouldBe -123
         }

         "be converted to the right value with big endian convention" in {
            ByteDataConverter.asSignedByte(Array(0x85.toByte), false) shouldBe -123
         }
      }
   }
}
