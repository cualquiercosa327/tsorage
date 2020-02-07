package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

import com.typesafe.config.Config
import spray.json.{JsBoolean, JsNumber, JsString, JsValue}

sealed abstract class ModbusDataType(
                                       val extractCode: String,
                                       val msgCode: String,
                                       val registerCount: Int,
                                       val highByteFirst: Boolean,
                                       val highWordFirst: Boolean
                                    )
{
   /**
    * Converts a normalized (big endian) byte array to a Json representation of its value.
    * @param bytes   A byte array.
    * @return        A Json representation of the value represented by bytes.
    */
   def bytesToJson(bytes: Array[Byte]): JsValue

   final def byteCount: Int = 2 * registerCount
}

abstract class NumericType(
                             val rank: Int,
                             extractCode: String,
                             msgCode: String,
                             registerCount: Int,
                             hbf: Boolean,
                             hwf: Boolean) extends ModbusDataType(extractCode, msgCode, registerCount, hbf, hwf)
{
   final override def bytesToJson(bytes: Array[Byte]): JsValue =
   {
      val raw = rawValue(bytes)
      val ranked = rank(raw)

      ranked
   }

   /**
    * Apply the effect of the rank on a raw valeu.
    * @param rawValue   A raw value
    * @return           The raw value, after being ranked
    */
   private def rank(rawValue: JsValue): JsValue =
   {
      if(rank == 0) rawValue
      else rawValue match {
         case JsNumber(x) => {
            JsNumber(x*Math.pow(10, rank))
         }
      }
   }

   protected def rawValue(bytes: Array[Byte]): JsValue

}

class Bool16(
               val position: Int,
               hbf: Boolean,
            ) extends ModbusDataType("bool16", "tbool", 1, hbf, true)
{
   override def bytesToJson(bytes: Array[Byte]): JsValue =
   {
      val value = ShortDataConverter.asUnsignedShort(bytes, !highByteFirst)
      val bit = ((value >> position) & 1) != 0
      JsBoolean(bit)
   }
}

class UByte(
              rank: Int,
              highByte: Boolean,
              hbf: Boolean
           ) extends NumericType(
   rank,
   "ubyte",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   1,
   hbf,
   true
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 2)

      val byte = if ((highByte && hbf) || (!highByte && !hbf)) bytes(0)
                 else bytes(1)

      JsNumber(ByteDataConverter.asUnsignedByte(Array(byte), false))
   }
}

class SByte(
              rank: Int,
              highByte: Boolean,
              hbf: Boolean
           ) extends NumericType(
   rank,
   "sbyte",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   1,
   hbf,
   true
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 2)

      val byte = if ((highByte && hbf) || (!highByte && !hbf)) bytes(0)
                 else bytes(1)

      JsNumber(ByteDataConverter.asSignedByte(Array(byte), false))
   }
}

class UInt16(
               rank: Int,
               hbf: Boolean
            ) extends NumericType(
   rank,
   "uint16",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   1,
   hbf,
   true
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 2)
      JsNumber(ShortDataConverter.asUnsignedShort(bytes, !hbf))
   }
}

class SInt16(
               rank: Int,
               hbf: Boolean
            ) extends NumericType(
   rank,
   "sint16",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   1,
   hbf,
   true
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 2)
      JsNumber(ShortDataConverter.asSignedShort(bytes, !highByteFirst))
   }
}

class UInt32(
               rank: Int,
               hbf: Boolean,
               hwf: Boolean
            ) extends NumericType(
   rank,
   "uint32",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   2,
   hbf,
   hwf
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 4)

      val ordered = DataConverter.orderNormalization(bytes, !highByteFirst, !highWordFirst)
      JsNumber(IntDataConverter.asUnsignedInt(ordered))
   }
}

class SInt32(
               rank: Int,
               hbf: Boolean,
               hwf: Boolean
            ) extends NumericType(
   rank,

   "sint32",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   2,
   hbf,
   hwf
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 4)

      val ordered = DataConverter.orderNormalization(bytes, !highByteFirst, !highWordFirst)
      JsNumber(IntDataConverter.asSignedInt(ordered))
   }
}

class SFloat32(
                 rank: Int,
                 hbf: Boolean,
                 hwf: Boolean
              ) extends NumericType(rank, "sfloat32", "tdouble", 2, hbf, hwf)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 4)

      val ordered = DataConverter.orderNormalization(bytes, !highByteFirst, !highWordFirst)
      JsNumber(FloatDataConverter.asSignedFloat(ordered))
   }
}

class Enum16(
               val dictionary: Map[Long, String],
               val mask: Option[Array[Byte]],
               hbf: Boolean
            ) extends ModbusDataType(
   "enum16",
   "ttext",
   1,
   hbf,
   true
)
{
   override def bytesToJson(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 2)

      val masked = mask match {
         case None => bytes
         case Some(m) => {
            val paddedMask = m.reverse.padTo(bytes.length, 0x0.toByte).reverse

            bytes
               .zip(paddedMask)
               .map{ case(a: Byte, b: Byte) => (a & b).toByte}
         }
      }

      val numericValue = ShortDataConverter.asUnsignedShort(masked, !highByteFirst)

      JsString(dictionary.getOrElse(numericValue, numericValue.toString))
   }
}

class Chararacter(
             val n: Int,
             hbf: Boolean,
             hwf: Boolean) extends ModbusDataType(s"char${n}", "ttext", n, hbf, hwf)
{
   /**
    * Converts a normalized (big endian) byte array to a Json representation of its value.
    *
    * @param bytes A byte array.
    * @return A Json representation of the value represented by bytes.
    */
   override def bytesToJson(bytes: Array[Byte]): JsValue =
   {
      assert(bytes.length == 2 * n)

      val ordered = DataConverter.orderNormalization(bytes, !highByteFirst, !highWordFirst)

      val text = (ordered.map(_.toChar)).mkString
      JsString(text)
   }
}

object ModbusDataType
{
   private val charRegex = """char(\d+)""".r

   def apply(extractConfig: Config): ModbusDataType =
   {
      val `type` = extractConfig.getString("type")

      val hbf = {
         if(extractConfig.hasPath("byte_order")) Some(extractConfig.getString("byte_order"))
         else None
         }.map(order => order match {
         case "HIGH_BYTE_FIRST" => true
         case _ => false
      }).getOrElse(true)

      val hwf = {
         if(extractConfig.hasPath("word_order")) Some(extractConfig.getString("word_order"))
         else None
         }.map(order => order match {
         case "HIGH_WORD_FIRST" => true
         case _ => false
      }).getOrElse(true)

      val dictionary: Map[Long, String] = if(extractConfig.hasPath("values")) extractConfig
         .getObject("values")
         .keySet.toArray
         .map(key => key.toString.toLong -> extractConfig.getConfig("values").getString(key.toString) )
         .toMap
                                         else Map.empty

      val rank = if(extractConfig.hasPath("rank")) extractConfig.getInt("rank")
                 else 0

      val mask: Option[Array[Byte]] = if(extractConfig.hasPath("mask"))
                                      {
                                         val mask = Integer.decode(extractConfig.getString("mask"))
                                         Some(BigInt(mask).toByteArray)
                                      }
                                      else None

      lazy val position = extractConfig.getInt("position")
      lazy val highByte = extractConfig.getString("byte") match {
         case "high" => true
         case _ => false
      }

      `type` match
      {
         case "ubyte" => new UByte(rank, highByte, hbf)
         case "bool16" => new Bool16(position, hbf)
         case "uint16" => new UInt16(rank, hbf)
         case "sint16" => new SInt16(rank, hbf)
         case "uint32" => new UInt32(rank, hbf, hwf)
         case "sint32" => new SInt32(rank, hbf, hwf)
         case "sfloat32" => new SFloat32(rank, hbf, hwf)
         case "enum16" => new Enum16(dictionary, mask, hbf)
         case charRegex(length) => new Chararacter(length.toInt, hbf, hwf)
      }
   }
}