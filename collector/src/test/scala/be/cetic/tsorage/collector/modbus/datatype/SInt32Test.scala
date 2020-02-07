package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.SInt32
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsNumber


class SInt32Test extends FlatSpec with Matchers
{
   private val negative_bytes = Array(0xca.toByte, 0xfe.toByte, 0xba.toByte, 0xfe.toByte)
   private val positive_bytes = Array(0x07.toByte, 0x5b.toByte, 0xcd.toByte, 0x15.toByte)

   "A SInt32 with HBF, HWF, negative bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, true, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-889275650L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, true, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-8892756500.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, true, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-88927565.0)
   }

   "A SInt32 with LBF, HWF, negative bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, false, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-20250950L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, false, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-202509500.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, false, true)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-2025095.0)
   }


   "A SInt32 with HBF, LWF, negative bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, true, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-1157707010L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, true, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber( -11577070100.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, true, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber( -115770701.0)
   }

   "A SInt32 with LBF, LWF, negative bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, false, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-21299510L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, false, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-212995100.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, false, false)
      dt.bytesToJson(negative_bytes) shouldBe JsNumber(-2129951.0)
   }


   // ====

   "A SInt32 with HBF, HWF, positive bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, true, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(123456789L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, true, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(1234567890.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, true, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(12345678.9)
   }

   "A SInt32 with LBF, HWF, positive bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, false, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(1527190989)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, false, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(15271909890.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, false, true)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(152719098.9)
   }


   "A SInt32 with HBF, LWF, positive bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, true, false)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(-854259877L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, true, false)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(-8542598770.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, true, false)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(-85425987.7)
   }

   "A SInt32 with LBF, LWF, positive bytes" should "provide the correct message for rank=0" in {
      val dt = new SInt32(0, false, false)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(365779719L)
   }

   it should "provide the correct message for positive rank" in {
      val dt = new SInt32(1, false, false)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(3657797190.0)
   }

   it should "provide the correct message for negative rank" in {
      val dt = new SInt32(-1, false, false)
      dt.bytesToJson(positive_bytes) shouldBe JsNumber(36577971.9)
   }
}
