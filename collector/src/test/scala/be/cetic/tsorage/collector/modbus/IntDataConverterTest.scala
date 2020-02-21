package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.data.IntDataConverter
import org.scalatest.{FlatSpec, Matchers, WordSpec}

/**
 * https://www.binaryconvert.com
 */
class IntDataConverterTest extends WordSpec with Matchers
{
   "A byte array" when {
      "representing a small unsigned int" should {
         "be converted to right value" in {
            IntDataConverter.asUnsignedInt(Array[Byte](0x00, 0x00, 0x07, 0xd0.toByte)) shouldBe 2000
         }
      }

      "representing a large unsigned int" should {
         "be converted to right value" in {
            IntDataConverter.asUnsignedInt(Array[Byte](0x01, 0x31, 0x2d, 0x00)) shouldBe 20000000
         }
      }

      "representing a small signed positive int" should {
         "be converted to right value" in {
            IntDataConverter.asSignedInt(Array[Byte](0x00, 0x00, 0x00, 0x2a)) shouldBe 42
         }
      }

      "representing a small signed negative int" should {
         "be converted to right value" in {
            IntDataConverter.asSignedInt(Array(0xff.toByte, 0xff.toByte, 0xff.toByte, 0xd6.toByte)) shouldBe -42
         }
      }

      "representing a large signed positive int" should {
         "be converted to right value" in {
            IntDataConverter.asSignedInt(Array(0x01, 0x31, 0x2d, 0x00)) shouldBe 20000000
         }
      }

      "representing a large signed negative int" should {
         "be converted to right value" in {
            IntDataConverter.asSignedInt(Array(0xfe.toByte, 0xce.toByte, 0xd3.toByte, 0x00)) shouldBe -20000000
         }
      }
   }
}
