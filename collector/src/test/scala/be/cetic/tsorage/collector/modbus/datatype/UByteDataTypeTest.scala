package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.UByte
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsNumber

class UByteDataTypeTest extends FlatSpec with Matchers
{
   "A UByte data type parameterized for the high byte first" should
      "provide the correct message value with High Byte First" in {
      val dt = new UByte(0, true, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(202)
   }

   it should "provide the correct message value with Low Byte First" in {
      val dt = new UByte(0, true, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(254)
   }

   "A UByte data type parameterized for the low byte first" should
      "provide the correct message value with High Byte First" in {
      val dt = new UByte(0, false, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(254)
   }

   it should "provide the correct message value with Low Byte First" in {
      val dt = new UByte(0, false, false)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(202)
   }

   "A UByte with a positive rank" should "provide the correct message value" in {
      val dt = new UByte(1, true, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(2020)
   }

   "A UByte with a negative rank" should "provide the correct message value" in {
      val dt = new UByte(-1, true, true)
      dt.bytesToJson(Array(0xca.toByte, 0xfe.toByte)) shouldBe JsNumber(20.2)
   }
}
