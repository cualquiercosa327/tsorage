package be.cetic.tsorage.collector.source

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

/**
 * A connector for Modbus RTU over TCP.
 */
class ModbusRTUTCPSource(config: Config) extends ModbusRTUSource(config) with LazyLogging
{
   override final protected def createConnectionFlow(config: Config)(implicit system: ActorSystem): Flow[ByteString, ByteString, _] =
   {
      val host: String = config.getString("host")
      val port: Int = config.getInt("port")

      Tcp().outgoingConnection(host, port)
   }
}
