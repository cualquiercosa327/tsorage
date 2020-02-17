package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.data.SInt16
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsNumber


class SUInt16Test extends FlatSpec with Matchers
{
   private val negative_bytes = Array(0xca.toByte, 0xfe.toByte)
   private val positive_bytes = Array(0x04.toByte, 0xd2.toByte)
   private val positive_bytes_reverse = Array(0xd2.toByte, 0x04.toByte)

   "A UInt16 with negative number" should "provide the correct message for rank=0, HBF" in {
      val dt = new SInt16(0, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-13570)
   }

   it should "provide the correct message for positive rank, HBF" in {
      val dt = new SInt16(1, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-135700.0)
   }

   it should "provide the correct message for negative rank, HBF" in {
      val dt = new SInt16(-1, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-1357.0)
   }

   it should "provide the correct message for rank=0, LBF" in {
      val dt = new SInt16(0, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-310)
   }

   it should "provide the correct message for positive rank, LBF" in {
      val dt = new SInt16(1, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-3100.0)
   }

   it should "provide the correct message for negative rank, LBF" in {
      val dt = new SInt16(-1, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-31.0)
   }


   "A UInt16 with positive number" should "provide the correct message for rank=0, HBF" in {
      val dt = new SInt16(0, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(1234)
   }

   it should "provide the correct message for positive rank, HBF" in {
      val dt = new SInt16(1, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(12340.0)
   }

   it should "provide the correct message for negative rank, HBF" in {
      val dt = new SInt16(-1, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(123.4)
   }

   it should "provide the correct message for rank=0, LBF" in {
      val dt = new SInt16(0, false)
      dt.bytesToJson(positive_bytes_reverse) shouldBe JsNumber(1234)
   }

   it should "provide the correct message for positive rank, LBF" in {
      val dt = new SInt16(1, false)
      dt.bytesToJson(positive_bytes_reverse) shouldBe JsNumber(12340.0)
   }

   it should "provide the correct message for negative rank, LBF" in {
      val dt = new SInt16(-1, false)
      dt.bytesToJson(positive_bytes_reverse) shouldBe JsNumber(123.4)
   }
}
