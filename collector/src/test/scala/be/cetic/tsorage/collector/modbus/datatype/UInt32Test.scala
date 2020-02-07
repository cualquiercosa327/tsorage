package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.UInt32
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsNumber


class UInt32Test extends FlatSpec with Matchers
{
   private val bytes = Array(0xca.toByte, 0xfe.toByte, 0xba.toByte, 0xfe.toByte)

   "A UInt32 with HBF, HWF" should "provide the correct message for rank=0" in {
      val dt = new UInt32(0, true, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(3405691646L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new UInt32(1, true, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(34056916460.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new UInt32(-1, true, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(340569164.6)
   }

   "A UInt32 with LBF, HWF" should "provide the correct message for rank=0" in {
      val dt = new UInt32(0, false, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(4274716346L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new UInt32(1, false, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(42747163460.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new UInt32(-1, false, true)
      dt.bytesToJson(bytes) shouldBe JsNumber(427471634.6)
   }


   "A UInt32 with HBF, LWF" should "provide the correct message for rank=0" in {
      val dt = new UInt32(0, true, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(3137260286L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new UInt32(1, true, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(31372602860.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new UInt32(-1, true, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(313726028.6)
   }

   "A UInt32 with LBF, LWF" should "provide the correct message for rank=0" in {
      val dt = new UInt32(0, false, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(4273667786L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new UInt32(1, false, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(42736677860.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new UInt32(-1, false, false)
      dt.bytesToJson(bytes) shouldBe JsNumber(427366778.6)
   }
}
