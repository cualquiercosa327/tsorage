package be.cetic.tsorage.collector.modbus.comm

import java.nio.ByteOrder

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString
import be.cetic.tsorage.collector.modbus.data.ByteDataConverter

/**
 * Created by Mathieu Goeminne.
 */
object ModbusFraming
{
   private val CRC_LENGTH = 2
   private val TCP_MESSAGE_LENGTH_LENGTH = 2
   private val RTU_MESSAGE_LENGTH_LENGTH = 1
   private val MAX_FIELD_LENGTH = 260


   val tcpFraming: Flow[ByteString, ByteString, NotUsed] = Framing.lengthField(
      TCP_MESSAGE_LENGTH_LENGTH,
      4,
      MAX_FIELD_LENGTH,
      ByteOrder.BIG_ENDIAN,
      {(offsetBytes: Array[Byte], computedSize: Int) => offsetBytes.length + TCP_MESSAGE_LENGTH_LENGTH + computedSize}
   )

   val rtuFraming: Flow[ByteString, ByteString, NotUsed] =
      Framing.lengthField(
      RTU_MESSAGE_LENGTH_LENGTH,
      2,
      MAX_FIELD_LENGTH,
      ByteOrder.BIG_ENDIAN,
      {(offsetBytes: Array[Byte], computedSize: Int) => offsetBytes.length +
         RTU_MESSAGE_LENGTH_LENGTH +
         CRC_LENGTH +
         computedSize
      }
   )
}
