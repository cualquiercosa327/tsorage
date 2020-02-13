package be.cetic.tsorage.collector.source

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, StreamConverters, Tcp}
import akka.util.ByteString
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.concurrent.duration.{Duration, FiniteDuration}
import collection.JavaConverters._
import com.fazecast.jSerialComm._

/**
 * A Modbus source, based on the good old RTU.
 *
 * This implementation is based on jSerialComm : https://fazecast.github.io/jSerialComm
 */
class ModbusRTUSerialSource(config: Config) extends ModbusRTUSource(config) with LazyLogging
{
   override protected def createConnectionFlow(config: Config)(implicit system: ActorSystem): Flow[ByteString, ByteString, _] =
   {
      val portName = config.getString("port")
      val availablePorts = SerialPort.getCommPorts.map(p => p.getSystemPortName())

      if(!(availablePorts contains portName))
      {
         logger.error(s"Specified serial port (${portName}) not in the list of available ports.")
         logger.error("Detected ports:")
         availablePorts.map(p => s"\t - ${p}").foreach(p => logger.error(p))
      }

      val baudRate = if(config.hasPath("baud_rate")) config.getInt("baud_rate")
                     else 9600

      val parity = if(config.hasPath("parity"))
                      config.getString("parity") match {
                         case "odd" =>  SerialPort.ODD_PARITY
                         case "even" => SerialPort.EVEN_PARITY
                         case "no" =>   SerialPort.NO_PARITY
                         case _ =>      SerialPort.EVEN_PARITY
                      }
                   else SerialPort.EVEN_PARITY

      val port = SerialPort.getCommPort(portName)

      port.openPort()

      port.setComPortParameters(
         baudRate,
         8,             // Must be 8 according to the specs
         SerialPort.ONE_STOP_BIT,    // Must be ONE_STOP_BIT according to the specs
         parity
      )

      Flow.fromSinkAndSource(
         StreamConverters.fromOutputStream(() => port.getOutputStream, autoFlush = true),
         StreamConverters.fromInputStream(() => port.getInputStream)
      )
   }
}
