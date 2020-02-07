package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.UInt16
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsNumber


class UInt16Test extends FlatSpec with Matchers
{
   private val bytes = Array(0xca.toByte, 0xfe.toByte)

   "A UInt16" should "provide the correct message for rank=0, HBF" in {
      val dt = new UInt16(0, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(51966)
   }

   it should "provide the correct message for positive rank, HBF" in {
      val dt = new UInt16(1, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(519660.0)
   }

   it should "provide the correct message for negative rank, HBF" in {
      val dt = new UInt16(-1, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(5196.6)
   }

   it should "provide the correct message for rank=0, LBF" in {
      val dt = new UInt16(0, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(65226)
   }

   it should "provide the correct message for positive rank, LBF" in {
      val dt = new UInt16(1, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(652260.0)
   }

   it should "provide the correct message for negative rank, LBF" in {
      val dt = new UInt16(-1, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(6522.6)
   }
}
