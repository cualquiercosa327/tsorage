package be.cetic.tsorage.collector.modbus

import org.scalatest.{FlatSpec, Matchers}

class RequestTest extends FlatSpec with Matchers
{
   "A RTU request for reading of coils (function 1)" should "be correctly converted to Modbus Frame" in {
      new ReadCoils(4, 10, 13).createRTUFrame() shouldBe
         Array[Byte](0x04, 0x01, 0x00, 0x0a, 0x00, 0x0d, 0xdd.toByte, 0x98.toByte)
   }

   "A RTU request for reading of discrete inputs (function 2)" should "be correctly converted to Modbus Frame" in {
      new ReadDiscreteInput(4, 10, 13).createRTUFrame() shouldBe
         Array[Byte](0x04, 0x02, 0x00, 0x0a, 0x00, 0x0d, 0x99.toByte, 0x98.toByte)
   }

   "A RTU request for reading of Holding registers (function 3)" should "be correctly converted to Modbus Frame" in {
      new ReadHoldingRegister(1, 0, 2).createRTUFrame() shouldBe
         Array[Byte](0x01, 0x03, 0x00, 0x00, 0x00, 0x02, 0xc4.toByte, 0x0b)
   }

   "Another RTU request for reading of Holding registers (function 3)" should "be correctly converted to Modbus Frame" in {
      new ReadHoldingRegister(1, 107, 3).createRTUFrame() shouldBe
         Array[Byte](0x01, 0x03, 0x00, 0x6b, 0x00, 0x03, 0x74.toByte, 0x17.toByte)
   }

   it should "be correctly converted to Modbus TCP" in {
      new ReadHoldingRegister(17, 107, 3).createTCPFrame(1) shouldBe
         Array[Byte](0x00, 0x01, 0x00, 0x00, 0x00, 0x06, 0x11, 0x03, 0x00, 0x6b.toByte, 0x00, 0x03)
   }

   "A RTU request for reading of Input Registers (function 4)" should "be correctly converted to Modbus Frame" in {
      new ReadInputRegister(1, 0, 2).createRTUFrame() shouldBe
      Array[Byte](0x01, 0x04, 0x00, 0x00, 0x00, 0x02, 0x71.toByte, 0xcb.toByte)
   }
}
