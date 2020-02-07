package be.cetic.tsorage.collector.modbus

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
      if(rank == 0) rawValue(bytes)
      else rank(rawValue(bytes))
   }

   protected def rawValue(bytes: Array[Byte]): JsValue

   /**
    * Apply the effect of the rank on a raw valeu.
    * @param rawValue   A raw value
    * @return           The raw value, after being ranked
    */
   private def rank(rawValue: JsValue): JsValue = rawValue match
   {
      case JsNumber(x) => {
         JsNumber(x*Math.pow(10, rank))
      }
   }
}

class Bool16(hbf: Boolean, hwf: Boolean) extends ModbusDataType("bool16", "tbool", 1, hbf, hwf)
{
   override def bytesToJson(bytes: Array[Byte]): JsValue =
      JsBoolean(ShortDataConverter.asUnsignedShort(bytes, !highByteFirst) != 0)
}

class UInt16(
               rank: Int,
               hbf: Boolean,
               hwf: Boolean
            ) extends NumericType(
   rank,
   "uint16",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   1,
   hbf,
   hwf
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
      JsNumber(ShortDataConverter.asUnsignedShort(bytes, !highByteFirst))
}

class SInt16(
               rank: Int,
               hbf: Boolean,
               hwf: Boolean
            ) extends NumericType(
   rank,
   "sint16",
   rank match {
      case 0 => "tlong"
      case _ => "tdouble"
   },
   1,
   hbf,
   hwf
)
{
   override protected def rawValue(bytes: Array[Byte]): JsValue =
      JsNumber(ShortDataConverter.asSignedShort(bytes, !highByteFirst))
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
      val ordered = DataConverter.orderNormalization(bytes, !highByteFirst, !highWordFirst)
      JsNumber(FloatDataConverter.asSignedFloat(ordered))
   }
}

class Enum16(
               val dictionary: Map[Int, String],
               hbf: Boolean,
               hwf: Boolean
            ) extends ModbusDataType("enum16", "ttext", 1, hbf, hwf)
{
   override def bytesToJson(bytes: Array[Byte]): JsValue =
   {
      val ordered = DataConverter.orderNormalization(bytes, !highByteFirst, !highWordFirst)
      val numericValue = ShortDataConverter.asUnsignedShort(ordered, false)

      JsString(dictionary.getOrElse(numericValue, numericValue.toString))
   }
}

class Char(
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
      val text = (bytes.map(_.toChar)).mkString
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

      val dictionary: Map[Int, String] = if(extractConfig.hasPath("values")) extractConfig
         .getObject("values")
         .keySet.toArray
         .map(key => key.toString.toInt -> extractConfig.getConfig("values").getString(key.toString) )
         .toMap
                                         else Map.empty

      val rank = if(extractConfig.hasPath("rank")) extractConfig.getInt("rank")
                 else 0

      `type` match {
         case "bool16" => new Bool16(hbf, hwf)
         case "uint16" => new UInt16(rank, hbf, hwf)
         case "sint16" => new SInt16(rank, hbf, hwf)
         case "uint32" => new UInt32(rank, hbf, hwf)
         case "sint32" => new SInt32(rank, hbf, hwf)
         case "sfloat32" => new SFloat32(rank, hbf, hwf)
         case "enum16" => new Enum16(dictionary, hbf, hwf)
         case charRegex(length) => new Char(length.toInt, hbf, hwf)
      }
   }
}