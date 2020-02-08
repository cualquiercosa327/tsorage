package be.cetic.tsorage.collector.source

import java.nio.ByteOrder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{BidiFlow, Flow, Framing, GraphDSL, Tcp}
import akka.util.ByteString
import be.cetic.tsorage.collector.modbus.{Extract, ModbusFunction}
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import GraphDSL.Implicits._
import be.cetic.tsorage.collector.modbus.comm.rtu.{ModbusRTUResponse, RTUMessageFactory}
import be.cetic.tsorage.collector.modbus.comm.tcp.ModbusTCPResponse
import be.cetic.tsorage.collector.modbus.comm.{ModbusFraming, ModbusRequest, ModbusResponseFactory}
import be.cetic.tsorage.common.WireTape

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * A Modbus RTU over TCP source.
 *
 **/
class ModbusRTUTCPSource(val config: Config) extends PollSource(
   Duration(config.getString("interval")) match {
      case fd: FiniteDuration => fd
      case _ =>  1 minute
   }
)
{
   /**
    * Generates a flow that reacts to incoming ticks by producing new messages.
    *
    * @return A flow that can produce new messages every time a tick is incoming.
    */
   override protected def buildPollFlow()
   (implicit ec: ExecutionContextExecutor, system: ActorSystem): Flow[String, Message, NotUsed] =
   {
      val host: String = config.getString("host")
      val port: Int = config.getInt("port")

      val extracts = Extract.fromSourceConfig(config)

      val requests: Map[ModbusFunction, List[ModbusRequest]] = extracts.map{
         case (f: ModbusFunction, fExtracts: List[Extract]) => f -> f.prepareRequests(fExtracts)
      }

      Flow.fromGraph(createGraph(host, port, requests, extracts))
   }

   private def createGraph(
                             host: String,
                             port: Int,
                             requests: Map[ModbusFunction, List[ModbusRequest]],
                             extracts: Map[ModbusFunction, List[Extract]]
                          )
   (implicit context: ExecutionContextExecutor, system: ActorSystem) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val modbusFraming = ModbusFraming.rtuFraming

         val indexedRequests = requests.mapValues(reqs =>
            reqs
               .map(entry => entry.unitId -> entry)
               .toMap
         )
         val requestFrames = requests
            .values
            .flatMap(requests => requests.map(request => (request, request.createRTUFrame())) )
            .toList

         val messageFactory = RTUMessageFactory(
            indexedRequests,
            extracts
         )

         val incoming: Flow[ByteString, ModbusRTUResponse, _] = Flow[ByteString]
            .via(modbusFraming)
            .via(Flow.fromFunction({response: ByteString => ModbusResponseFactory.fromRTUByteString(response)}))

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
            Flow[ModbusRTUResponse]
               .mapConcat(messageFactory.responseToMessages)
               .buffer(300, OverflowStrategy.backpressure)
         )

         requestFlow ~> Flow[(ModbusRequest, Array[Byte])].map(_._2) ~> modbusFlow ~> messageFlow

         FlowShape(requestFlow.in, messageFlow.out)
   }
}
