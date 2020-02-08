package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.data.IntDataConverter
import org.scalatest.{FlatSpec, Matchers}

/**
 * https://www.binaryconvert.com
 */
class IntDataConverterTest extends FlatSpec with Matchers
{


   "A byte array representing a small unsigned int" should "be converted into the right value" in {
      IntDataConverter.asUnsignedInt(Array[Byte](0x00, 0x00, 0x07, 0xd0.toByte)) shouldBe 2000
   }

   "A byte array representing a large unsigned int" should "be converted into the right value" in {
      IntDataConverter.asUnsignedInt(Array[Byte](0x01, 0x31, 0x2d, 0x00)) shouldBe 20000000
   }

   "A byte array representing a small positive signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array[Byte](0x00, 0x00, 0x00, 0x2a)) shouldBe 42
   }

   "A byte array representing a small negative signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array(0xff.toByte, 0xff.toByte, 0xff.toByte, 0xd6.toByte)) shouldBe -42
   }

   "A byte array representing a large positive signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array(0x01, 0x31, 0x2d, 0x00)) shouldBe 20000000
   }


   "A byte array representing a large negative signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array(0xfe.toByte, 0xce.toByte, 0xd3.toByte, 0x00)) shouldBe -20000000
   }
}
