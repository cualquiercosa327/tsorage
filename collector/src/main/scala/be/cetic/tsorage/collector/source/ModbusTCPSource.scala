package be.cetic.tsorage.collector.source

import akka.NotUsed
import akka.stream.scaladsl.{BidiFlow, Flow, Framing, GraphDSL, Tcp}
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.collector.modbus._
import com.typesafe.config.Config
import java.net._
import java.nio.ByteOrder

import akka.actor.ActorSystem
import akka.util.ByteString
import be.cetic.tsorage.collector.modbus.{MessageFactory, ModbusRequest, ModbusResponse, ModbusResponseFactory, ReadCoilsRequest, ReadDiscreteInputRequest, ReadHoldingRegisterRequest, ReadInputRegisterRequest}
import GraphDSL.Implicits._
import akka.stream.{FlowShape, OverflowStrategy}
import be.cetic.tsorage.common.WireTape

import scala.concurrent.duration._
import collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.util.Random


/**
 * Implements a Modbus TCP master (client),
 * that extract values by submitting read requests to Modbus slaves (servers).
 *
 *                                     TCP Connexion
 *
 *                                      ^  O
 *                                      |  |
 *            +-------------------------|--|-----+
 *            |    +------+             |  |     |
 *   tick     |    |      | TCP Request |  |     |
 * O ------------->|      | ------------+  |     |
 *            |    |      |                |   +----->
 *            | +--|      | <--------------+   | |
 *            | |  |      |  TCP Response      | |
 *            | |  +------+                    | |
 *            | +------------------------------+ |
 *            +----------------------------------+
 */
class ModbusTCPSource(val config: Config) extends PollSource(
   Duration(config.getString("interval")) match {
      case fd: FiniteDuration => fd
      case _ =>  1 minute
   }
)
{
   override protected def buildPollFlow()
   (implicit ec: ExecutionContextExecutor, system: ActorSystem): Flow[String, Message, NotUsed] =
   {
      val host: String = config.getString("host")
      val port: Int = config.getInt("port")
      val unitId: Int = config.getInt("unit_id")

      val extracts: Map[ModbusFunction, List[Extract]] = List(
         ReadCoils,
         ReadInputRegister,
         ReadDiscreteInput,
         ReadHoldingRegister
      ).map(f => f-> {
         val name = f.extractName
         if(config.hasPath(name)) config.getConfigList(name).asScala.toList.map(Extract(_))
         else List.empty
      }).toMap

      val requests: Map[ModbusFunction, List[ModbusRequest]] = extracts.map{
         case (f: ModbusFunction, extracts: List[Extract]) => f -> f.prepareRequests(unitId, extracts)
      }

      Flow.fromGraph(createGraph(host, port, requests, extracts, unitId))
   }

   private def createGraph(
                             host: String,
                             port: Int,
                             requests: Map[ModbusFunction, List[ModbusRequest]],
                             extracts: Map[ModbusFunction, List[Extract]],
                             unitId: Int
                          )
   (implicit context: ExecutionContextExecutor, system: ActorSystem) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

      val modbusFraming = Framing.lengthField(
         2,
         4,
         260,
         ByteOrder.BIG_ENDIAN,
         {(offsetBytes: Array[Byte], computedSize: Int) => offsetBytes.length + 2 + computedSize}
      )

      val indexedRequests = requests.mapValues(reqs =>
         reqs
            .zipWithIndex
            .map(entry => (entry._1.unitId,  entry._2) -> entry._1)
            .toMap
      )

      val requestFrames = indexedRequests
            .values
            .flatMap(m => m.map{ case ((unitId: Int, index: Int), request: ModbusRequest) => request.createTCPFrame(index)} )
            .toList

      val messageFactory = MessageFactory(
         indexedRequests,
         extracts,
         unitId
      )

      val incoming: Flow[ByteString, ModbusResponse, _] = Flow[ByteString]
         .via(modbusFraming)
         .via(Flow.fromFunction({response: ByteString => ModbusResponseFactory.fromByteString(response)}))

      val outgoing:Flow[Array[Byte], ByteString, _] = Flow fromFunction {request: Array[Byte] => ByteString(request)}

      val protocol = BidiFlow.fromFlows(incoming, outgoing)

      val modbusFlow = builder.add(
         Tcp()
            .outgoingConnection(host, port)
            .join(protocol)
      )

      val requestFlow = builder.add(
         Flow[String]      // Converts ticks to modbus request frames
         .mapConcat(tick => requestFrames)
      )

      val messageFlow = builder.add(
         Flow[ModbusResponse]
            .mapConcat(messageFactory.responseToMessages)
            .buffer(300, OverflowStrategy.backpressure)
      )

      requestFlow ~> modbusFlow ~> messageFlow

      FlowShape(requestFlow.in, messageFlow.out)
   }
}

