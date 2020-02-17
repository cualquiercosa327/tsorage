package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.data.Bool16
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsBoolean


class Bool16Test extends FlatSpec with Matchers
{
   "A Bool16 applied on HBF byte array" should "produce the correct message for bit 0" in {
      val dt = new Bool16(0, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 1" in {
      val dt = new Bool16(1, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 2" in {
      val dt = new Bool16(2, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 3" in {
      val dt = new Bool16(3, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 4" in {
      val dt = new Bool16(4, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 5" in {
      val dt = new Bool16(5, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 6" in {
      val dt = new Bool16(6, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 7" in {
      val dt = new Bool16(7, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 8" in {
      val dt = new Bool16(8, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 9" in {
      val dt = new Bool16(9, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 10" in {
      val dt = new Bool16(10, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 11" in {
      val dt = new Bool16(11, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 12" in {
      val dt = new Bool16(12, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 13" in {
      val dt = new Bool16(13, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 14" in {
      val dt = new Bool16(14, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 15" in {
      val dt = new Bool16(15, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }
   
   // =============

   "A Bool16 applied on LBF byte array" should "produce the correct message for bit 0" in {
      val dt = new Bool16(0, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 1" in {
      val dt = new Bool16(1, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 2" in {
      val dt = new Bool16(2, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 3" in {
      val dt = new Bool16(3, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 4" in {
      val dt = new Bool16(4, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 5" in {
      val dt = new Bool16(5, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 6" in {
      val dt = new Bool16(6, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 7" in {
      val dt = new Bool16(7, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 8" in {
      val dt = new Bool16(8, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(false)
   }

   it should "produce the correct message for bit 9" in {
      val dt = new Bool16(9, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 10" in {
      val dt = new Bool16(10, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 11" in {
      val dt = new Bool16(11, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 12" in {
      val dt = new Bool16(12, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 13" in {
      val dt = new Bool16(13, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 14" in {
      val dt = new Bool16(14, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }

   it should "produce the correct message for bit 15" in {
      val dt = new Bool16(15, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsBoolean(true)
   }
}
