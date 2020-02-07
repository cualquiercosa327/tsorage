package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.SByte
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsNumber

class SByteDataTypeTest extends FlatSpec with Matchers
{
   "A SByte data type parameterized for the high byte first" should
      "provide the correct message value with High Byte First, for negative number" in {
      val dt = new SByte(0, true, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(-54)
   }

   it should "provide the correct message value with High Byte First, for positive number" in {
      val dt = new SByte(0, true, true)
      dt.bytesToJson(Array(0x2a.toByte, 0xfe.toByte)) shouldBe JsNumber(42)
   }

   it should "provide the correct message value with Low Byte First, for negative number" in {
      val dt = new SByte(0, true, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(-2)
   }

   it should "provide the correct message value with Low Byte First, for positive number" in {
      val dt = new SByte(0, true, false)
      dt.bytesToJson(Array(0xca.toByte, 0x2a.toByte)) shouldBe JsNumber(42)
   }

   "A SByte data type parameterized for the low byte first" should
      "provide the correct message value with High Byte First, for negative number" in {
      val dt = new SByte(0, false, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(-2)
   }

   it should "provide the correct message value with High Byte First, for positive number" in {
      val dt = new SByte(0, false, true)
      dt.bytesToJson(Array(0xca.toByte, 0x2a.toByte)) shouldBe JsNumber(42)
   }

   it should "provide the correct message value with Low Byte First, for negative number" in {
      val dt = new SByte(0, false, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(-54)
   }

   it should "provide the correct message value with Low Byte First, for positive number" in {
      val dt = new SByte(0, false, false)
      dt.bytesToJson(Array(0x2a.toByte, 0xfe.toByte)) shouldBe JsNumber(42)
   }

   "A SByte with a positive rank" should "provide the correct message value, for negative number" in {
      val dt = new SByte(1, true, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(-540)
   }

   it should "provide the correct message value, for positive number" in {
      val dt = new SByte(1, true, true)
      dt.bytesToJson(Array(0x2a.toByte, 0xfe.toByte)) shouldBe JsNumber(420)
   }

   "A SByte with a negative rank" should "provide the correct message value, for negative number" in {
      val dt = new SByte(-1, true, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(-5.4)
   }

   it should "provide the correct message value, for positive number" in {
      val dt = new SByte(-1, true, true)
      dt.bytesToJson(Array(0x2a.toByte, 0xfe.toByte)) shouldBe JsNumber(4.2)
   }
}
