package be.cetic.tsorage.collector.modbus

import org.scalatest.{FlatSpec, Matchers}


class RequestTest extends FlatSpec with Matchers
{
   "A reading of coils (function 1)" should "be correctly converted to Modbus Frame" in {
      new ReadCoils(10, 13).createRTUFrame(4) shouldBe
         Array[Byte](0x04, 0x01, 0x00, 0x0a, 0x00, 0x0d, 0xdd.toByte, 0x98.toByte)
   }

   "A reading of discrete inputs (function 2)" should "be correctly converted to Modbus Frame" in {
      new ReadDiscreteInput(10, 13).createRTUFrame(4) shouldBe
         Array[Byte](0x04, 0x02, 0x00, 0x0a, 0x00, 0x0d, 0x99.toByte, 0x98.toByte)
   }

   "A reading of Holding registers (function 3)" should "be correctly converted to Modbus Frame" in {
      new ReadHoldingRegister(0, 2).createRTUFrame(1) shouldBe
         Array[Byte](0x01, 0x03, 0x00, 0x00, 0x00, 0x02, 0xc4.toByte, 0x0b)
   }

   "A reading of Input Registers (function 4)" should "A reading of Input Registers (function 4)" in {
      new ReadInputRegister(0, 2).createRTUFrame(1) shouldBe
      Array[Byte](0x01, 0x04, 0x00, 0x00, 0x00, 0x02, 0x71.toByte, 0xcb.toByte)
   }
}
