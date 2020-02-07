package be.cetic.tsorage.collector.modbus

import org.scalatest.{FlatSpec, Matchers}

/**
 * https://docklight.de/manual/sendingmodbuscommandswithc.htm
 */
class CRC16Test extends FlatSpec with Matchers
{
   val array = Array[Byte](0x01, 0x04, 0x02, 0xFF.toByte, 0xFF.toByte)

   "The CRC of an arbitrary byte array" should "be correct" in {
      CRC16.calculateCRC(Array[Byte](0x01, 0x04, 0x02, 0xFF.toByte, 0xFF.toByte)) shouldBe
         Array[Byte](0xb8.toByte, 0x80.toByte)
   }

   it should "be correct 2" in {
      CRC16.calculateCRC(Array[Byte](0x02, 0x04, 0x02, 0x7F, 0x58)) shouldBe
         Array[Byte](0xdc.toByte, 0xfa.toByte)
   }

   it should "be correct 3" in {
      CRC16.calculateCRC(Array[Byte](0x03, 0x04, 0x02, 0x01, 0x0A)) shouldBe
         Array[Byte](0x41.toByte, 0x67.toByte)
   }

   it should "be correct 4" in {
      CRC16.calculateCRC(Array[Byte](0x04, 0x04, 0x02, 0x40, 0x00)) shouldBe
         Array[Byte](0x44.toByte, 0xf0.toByte)
   }
}
