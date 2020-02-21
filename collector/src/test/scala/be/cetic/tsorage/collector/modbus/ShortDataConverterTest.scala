package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.data.ShortDataConverter
import org.scalatest.{FlatSpec, Matchers, WordSpec}

/**
 * https://www.binaryconvert.com
 */
class ShortDataConverterTest extends WordSpec with Matchers
{
   "A small unsigned short" when {
      "converted to little endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromUnsignedShort(42, true) shouldBe Array[Byte](0x2a, 0)
         }
      }

      "converted to big endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromUnsignedShort(42, false) shouldBe Array[Byte](0, 0x2a)
         }
      }
   }

   "A large unsigned short" when {
      "converted to little endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromUnsignedShort(2000, true) shouldBe Array[Byte](0xd0.toByte, 0x07.toByte)
         }
      }

      "converted to big endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromUnsignedShort(2000, false) shouldBe Array[Byte](0x07.toByte, 0xd0.toByte)
         }
      }
   }

   "A small signed positive short" when {
      "converted to little endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(42, true) shouldBe Array[Byte](0x2a.toByte, 0x00.toByte)
         }
      }

      "converted to big endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(42, false) shouldBe Array[Byte](0x00.toByte, 0x2a.toByte)
         }
      }
   }

   "A small signed negative short" when {
      "converted to little endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(-42, true) shouldBe Array[Byte](0xd6.toByte, 0xff.toByte)
         }
      }

      "converted to big endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(-42, false) shouldBe Array[Byte](0xff.toByte, 0xd6.toByte)
         }
      }
   }

   "A large signed positive short" when {
      "converted to little endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(2000, true) shouldBe Array[Byte](0xd0.toByte, 0x07.toByte)
         }
      }

      "converted to big endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(2000, false) shouldBe Array[Byte](0x07.toByte, 0xd0.toByte)
         }
      }
   }


   "A large signed negative short" when {
      "converted to little endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(-2000, true) shouldBe Array[Byte](0x30.toByte, 0xf8.toByte)
         }
      }

      "converted to big endian byte array" should {
         "be correctly converted" in {
            ShortDataConverter.fromSignedShort(-2000, false) shouldBe Array[Byte](0xf8.toByte, 0x30.toByte)
         }
      }
   }

   "A little endian byte array" when {
      "representing a small unsigned short" should {
         "be converted to the right value " in {
            ShortDataConverter.asUnsignedShort(Array(0x2a.toByte, 0x00.toByte), true) shouldBe 42
         }
      }

      "representing a large unsigned short" should {
         "be converted to the right value " in {
            ShortDataConverter.asUnsignedShort(Array(0xd0.toByte, 0x07.toByte), true) shouldBe 2000
         }
      }

      "representing a small signed positive short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0x2a, 0x0), true) shouldBe 42
         }
      }

      "representing a small signed negative short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0xd6.toByte, 0xff.toByte), true) shouldBe -42
         }
      }

      "representing a large signed positive short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0xd0.toByte, 0x07.toByte), true) shouldBe 2000
         }
      }

      "representing a large signed negative short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0x30.toByte, 0xf8.toByte), true) shouldBe -2000
         }
      }
   }

   "A big endian endian byte array" when {
      "representing a small unsigned short" should {
         "be converted to the right value " in {
            ShortDataConverter.asUnsignedShort(Array(0x00.toByte, 0x2a.toByte), false) shouldBe 42
         }
      }

      "representing a large unsigned short" should {
         "be converted to the right value " in {
            ShortDataConverter.asUnsignedShort(Array(0x07.toByte, 0xd0.toByte), false) shouldBe 2000
         }
      }

      "representing a small signed positive short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0x0, 0x2a), false) shouldBe 42
         }
      }

      "representing a small signed negative short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0xff.toByte, 0xd6.toByte), false) shouldBe -42
         }
      }

      "representing a large signed positive short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0x07.toByte, 0xd0.toByte), false) shouldBe 2000
         }
      }

      "representing a large signed negative short" should {
         "be converted to the right value " in {
            ShortDataConverter.asSignedShort(Array(0xf8.toByte, 0x30.toByte), false) shouldBe -2000
         }
      }
   }
}
