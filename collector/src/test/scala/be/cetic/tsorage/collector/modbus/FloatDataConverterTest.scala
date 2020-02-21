package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.data.FloatDataConverter
import org.scalatest.{FlatSpec, Matchers, WordSpec}

class FloatDataConverterTest extends WordSpec with Matchers
{
   "A byte array" when {
      "representing a positive float" should {
         "be correctly converted to float" in {
            FloatDataConverter.asSignedFloat(
               Array[Byte](0x44.toByte, 0xa7.toByte, 0x2d.toByte, 0x71.toByte)
            ) shouldBe 1337.42F +- 0.01F
         }
      }

      "representing a negative float" should {
         "be correctly converted to float" in {
            FloatDataConverter.asSignedFloat(
               Array[Byte](0xc4.toByte, 0xa7.toByte, 0x2d.toByte, 0x71.toByte)
            ) shouldBe -1337.42F +- 0.01F
         }
      }
   }
}