package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.Chararacter
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsString

class ChararacterTest extends FlatSpec with Matchers
{
   private val bytes = Array(0x43.toByte, 0x41.toByte, 0x46.toByte, 0x45.toByte)

   "A char" should "provide the correct message value with HBF, HWF" in
   {
      val dt = new Chararacter(2, true, true)
      dt.bytesToJson(bytes) shouldBe JsString("CAFE")
   }

   it should "provide the correct message value with LBF, HWF" in
   {
      val dt = new Chararacter(2, false, true)
      dt.bytesToJson(bytes) shouldBe JsString("ACEF")
   }

   it should "provide the correct message value with HBF, LWF" in
   {
      val dt = new Chararacter(2, true, false)
      dt.bytesToJson(bytes) shouldBe JsString("FECA")
   }

   it should "provide the correct message value with LBF, LWF" in
   {
      val dt = new Chararacter(2, false, false)
      dt.bytesToJson(bytes) shouldBe JsString("EFAC")
   }
}
