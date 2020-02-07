package be.cetic.tsorage.collector.modbus.datatype

import be.cetic.tsorage.collector.modbus.SFloat32
import org.scalatest.{FlatSpec, Matchers}
import spray.json.JsNumber

class SFloat32Test extends FlatSpec with Matchers
{
   private val negative_bytes = Array(0xca.toByte, 0xfe.toByte, 0xba.toByte, 0xfe.toByte)
   private val positive_bytes = Array(0x46.toByte, 0x40.toByte, 0xe6.toByte, 0xb7.toByte)

   "A SFloat32 with HBF, HWF, negative bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, true, true)
         dt.bytesToJson(negative_bytes) shouldBe JsNumber(-8.347007E6)
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, true, true)
         dt.bytesToJson(negative_bytes) shouldBe JsNumber(-8.347007E7)
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, true, true)
         dt.bytesToJson(negative_bytes) shouldBe JsNumber(-8.347007E5)
      }

   "A SFloat32 with LBF, HWF, negative bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, false, true)
         dt.bytesToJson(negative_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-1.34913335539403565877457313216E38.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, false, true)
         dt.bytesToJson(negative_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-1.34913335539403565877457313216E39.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, false, true)
         dt.bytesToJson(negative_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-1.34913335539403565877457313216E37.toFloat +- 0.000001.toFloat)
         }
      }


   "A SFloat32 with HBF, LWF, negative bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, true, false)
         dt.bytesToJson(negative_bytes) shouldBe JsNumber(-1.94391584955155849456787109375E-3)
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, true, false)
         dt.bytesToJson(negative_bytes) shouldBe JsNumber(-1.94391584955155849456787109375E-2)
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, true, false)
         dt.bytesToJson(negative_bytes) shouldBe JsNumber(-1.94391584955155849456787109375E-4)
      }

   "A SFloat32 with LBF, LWF, negative bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, false, false)

         dt.bytesToJson(negative_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-1.2427967383240E38.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, false, false)
         dt.bytesToJson(negative_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-1.2427967383240E39.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, false, false)
         dt.bytesToJson(negative_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-1.2427967383240E37.toFloat +- 0.000001.toFloat)
         }
      }


   // ====

   "A SFloat32 with HBF, HWF, positive bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, true, true)
         dt.bytesToJson(positive_bytes) shouldBe JsNumber(1.23456787109375E4)
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, true, true)
         dt.bytesToJson(positive_bytes) shouldBe JsNumber(1.23456787109375E5)
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, true, true)
         dt.bytesToJson(positive_bytes) shouldBe JsNumber(1.23456787109375E3)
      }

   "A SFloat32 with LBF, HWF, positive bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, false, true)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (3.104974269866943359375.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, false, true)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (31.04974269866943359375.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, false, true)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (0.3104974269866943359375.toFloat +- 0.000001.toFloat)
         }
      }


   "A SFloat32 with HBF, LWF, positive bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, true, false)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-4.32744475068161585053696E23.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, true, false)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-4.32744475068161585053696E24.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, true, false)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-4.32744475068161585053696E22.toFloat +- 0.000001.toFloat)
         }
      }

   "A SFloat32 with LBF, LWF, positive bytes" should "provide the correct message for rank=0" in
      {
         val dt = new SFloat32(0, false, false)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-2.74480662483256310224533081055E-5.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for positive rank" in
      {
         val dt = new SFloat32(1, false, false)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-2.74480662483256310224533081055E-4.toFloat +- 0.000001.toFloat)
         }
      }

   it should "provide the correct message for negative rank" in
      {
         val dt = new SFloat32(-1, false, false)
         dt.bytesToJson(positive_bytes) match
         {
            case JsNumber(x) => x.toFloat shouldBe (-2.74480662483256310224533081055E-6.toFloat +- 0.000001.toFloat)
         }
      }
}
