package be.cetic.tsorage.collector.modbus

import jnr.ffi.mapper.DataConverter
import org.scalatest.{FlatSpec, Matchers}

class DataConverterTest extends FlatSpec with Matchers
{
   private val original = Array[Byte](0x00, 0x01, 0x02, 0x03)

   "No byte swap, no word swap" should "lead to an unchanged byte array" in {
      val in = DataConverter.orderNormalization(
         original, false, false
      ) shouldBe original
   }

   "No byte swap, word swap" should "lead to an unchanged byte array" in {
      val in = DataConverter.orderNormalization(
         original, false, true
      ) shouldBe Array[Byte](0x02, 0x03, 0x00, 0x01)
   }

   "byte swap, no word swap" should "lead to an unchanged byte array" in {
      val in = DataConverter.orderNormalization(
         original, true, false
      ) shouldBe Array[Byte](0x01, 0x00, 0x03, 0x02)
   }

   "byte swap, word swap" should "lead to an unchanged byte array" in {
      val in = DataConverter.orderNormalization(
         original, true, true
      ) shouldBe Array[Byte](0x03, 0x02, 0x01, 0x00)
   }

   "Conversion from large byte to unsigned int" should "pass" in {
      DataConverter.bytesToUnsignedInt(Array(
         0xca.toByte,
         0xfe.toByte,
         0xba.toByte,
         0xfe.toByte
      ),
         false
      ) shouldBe 3405691646L

   }
}
