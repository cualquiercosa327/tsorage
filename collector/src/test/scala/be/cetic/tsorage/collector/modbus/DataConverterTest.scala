package be.cetic.tsorage.collector.modbus

import jnr.ffi.mapper.DataConverter
import org.scalatest.{FlatSpec, Matchers, WordSpec}

class DataConverterTest extends WordSpec with Matchers
{
   private val original = Array[Byte](0x00, 0x01, 0x02, 0x03)

   "A byte array" when {
      "swapped with no byte swap, no word swap" should {
         "be correctly ordered" in {
            data.DataConverter.orderNormalization(
               original, false, false
            ) shouldBe original
         }
      }

      "swapped with no byte swap, word swap" should {
         "be correctly ordered" in {
            data.DataConverter.orderNormalization(
               original, false, true
            ) shouldBe Array[Byte](0x02, 0x03, 0x00, 0x01)
         }
      }

      "swapped with byte swap, no word swap" should {
         "be correctly ordered" in {
            data.DataConverter.orderNormalization(
               original, true, false
            ) shouldBe Array[Byte](0x01, 0x00, 0x03, 0x02)
         }
      }

      "swapped with byte swap, word swap" should {
         "be correctly ordered" in {
            data.DataConverter.orderNormalization(
               original, true, true
            ) shouldBe Array[Byte](0x03, 0x02, 0x01, 0x00)
         }
      }
   }

   "A large byte" when {
      "converted to unsigned int" should {
         "provide the correct value" in {
            data.DataConverter.bytesToUnsignedInt(Array(
               0xca.toByte,
               0xfe.toByte,
               0xba.toByte,
               0xfe.toByte
            ),
               false
            ) shouldBe 3405691646L
         }
      }
   }
}
