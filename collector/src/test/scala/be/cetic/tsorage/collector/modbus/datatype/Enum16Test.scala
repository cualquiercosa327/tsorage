package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.data.Enum16
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsString


class Enum16Test extends FlatSpec with Matchers
{
   private val bytes = Array(0xca.toByte, 0xfe.toByte)

   "An Enum16 with no mask" should "provide the correct message value for HBF" in
   {
      val dt = new Enum16(Map(123456789L -> "Test A", 51966L -> "Test B"), None, true)
      dt.bytesToJson(bytes) shouldBe JsString("Test B")
   }

   it should "provide the correct message value for LBF" in
   {
      val dt = new Enum16(Map(65226L -> "Test A", 51966L -> "Test B"), None, false)
      dt.bytesToJson(bytes) shouldBe JsString("Test A")
   }

   "An Enum16 with mask" should "provide the correct message value for HBF" in
   {
      val dt = new Enum16(
         Map(0xca.toLong -> "Test A", 0xfe.toLong -> "Test B"),
         Some(Array(0x00, 0xff.toByte)),
         true
      )

      dt.bytesToJson(bytes) shouldBe JsString("Test B")
   }

   it should "provide the correct message value for LBF" in
      {
         val dt = new Enum16(
            Map(0xca.toLong -> "Test A", 0xfe.toLong -> "Test B"),
            Some(Array(0xff.toByte, 0x00.toByte)),
            false
         )

         dt.bytesToJson(bytes) shouldBe JsString("Test A")
      }
}
