package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.data.FloatDataConverter
import org.scalatest.{FlatSpec, Matchers}

class FloatDataConverterTest extends FlatSpec with Matchers
{
   "A byte array representing a positive float" should "be correctly converted to a float" in {
      FloatDataConverter.asSignedFloat(
         Array[Byte](0x44.toByte, 0xa7.toByte, 0x2d.toByte, 0x71.toByte)
      ) shouldBe 1337.42F +- 0.01F
   }

   "A byte array representing a negative float" should "be correctly converted to a float" in {
      FloatDataConverter.asSignedFloat(
         Array[Byte](0xc4.toByte, 0xa7.toByte, 0x2d.toByte, 0x71.toByte)
      ) shouldBe -1337.42F +- 0.01F
   }
}