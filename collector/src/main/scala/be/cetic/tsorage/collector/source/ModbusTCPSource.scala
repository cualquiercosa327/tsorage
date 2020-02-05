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
import be.cetic.tsorage.collector.modbus.{MessageFactory, ModbusRequest, ModbusResponse, ModbusResponseFactory, ReadCoils, ReadDiscreteInput, ReadHoldingRegister, ReadInputRegister}
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


   private def prepareReadCoilsRequest(unitId: Int, registerConfig: List[Config]) =
   {
      registerConfig.map(regConf => {
         new ReadCoils(
            unitId,
            regConf.getInt("address"),
            typeToRegisterNumber(regConf.getString("type"))
         )
      })
   }

   private def prepareHoldingRegisterRequest(unitId: Int, registerConfig: List[Config]) =
   {
      registerConfig.map(regConf => {
         new ReadHoldingRegister(
            unitId,
            regConf.getInt("address"),
            typeToRegisterNumber(regConf.getString("type"))
         )
      })
   }

   private def prepareDiscreteInputRequests(unitId: Int, registerConfig: List[Config]) =
   {
      registerConfig.map(regConf => {
         new ReadDiscreteInput(
            unitId,
            regConf.getInt("address"),
            typeToRegisterNumber(regConf.getString("type"))
         )
      })
   }

   private def prepareInputRegisterRequest(unitId: Int, registerConfig: List[Config]) =
   {
      registerConfig.map(regConf => {
         new ReadInputRegister(
            unitId,
            regConf.getInt("address"),
            typeToRegisterNumber(regConf.getString("type"))
         )
      })
   }

   override protected def buildPollFlow()
   (implicit ec: ExecutionContextExecutor, system: ActorSystem): Flow[String, Message, NotUsed] =
   {
      val host: String = config.getString("host")
      val port: Int = config.getInt("port")
      val unitId: Int = config.getInt("unit_id")

      val readCoilsRequests = prepareReadCoilsRequest(
         unitId,
         if(config.hasPath("output_coils")) config.getConfigList("output_coils").asScala.toList
         else List.empty
      )

      val readInputRegisterRequests = prepareInputRegisterRequest(
         unitId,
         if(config.hasPath("input_register")) config.getConfigList("input_registers").asScala.toList
         else List.empty
      )

      val readDiscreteInputRequests = prepareDiscreteInputRequests(
         unitId,
         if(config.hasPath("input_contacts")) config.getConfigList("input_contacts").asScala.toList
         else List.empty
      )

      val readHoldingRegisterRequests = prepareHoldingRegisterRequest(
         unitId,
         if(config.hasPath("holding_registers")) config.getConfigList("holding_registers").asScala.toList
         else List.empty
      )

      Flow.fromGraph(createGraph(
         host,
         port,
         readCoilsRequests,
         readDiscreteInputRequests,
         readHoldingRegisterRequests,
         readInputRegisterRequests,
         config
      ))
   }

   private def createGraph(
                             host: String,
                             port: Int,
                             readCoilsRequests: List[ReadCoils],
                             readDiscreteInputRequests: List[ReadDiscreteInput],
                             readHoldingRegisterRequests: List[ReadHoldingRegister],
                             readInputRegisterRequests: List[ReadInputRegister],
                             deviceConfig: Config
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

      val indexedReadCoilsRequests = readCoilsRequests
         .zipWithIndex
         .map(entry => (entry._1.unitId,  entry._2) -> entry._1)
         .toMap

      val indexedReadDiscreteInputRequests = readDiscreteInputRequests
         .zipWithIndex
         .map(entry => (entry._1.unitId,  entry._2) -> entry._1)
         .toMap

      val indexedReadHoldingRegisterRequests = readHoldingRegisterRequests
         .zipWithIndex
         .map(entry => (entry._1.unitId,  entry._2) -> entry._1)
         .toMap

      val indexedReadInputRegisterRequests = readInputRegisterRequests
         .zipWithIndex
         .map(entry => (entry._1.unitId,  entry._2) -> entry._1)
         .toMap

      val requestFrames = List(
         indexedReadCoilsRequests,
         indexedReadDiscreteInputRequests,
         indexedReadHoldingRegisterRequests,
         indexedReadInputRegisterRequests
      )
      .flatMap(requests => requests.toSeq)
      .map{ case ((unitId: Int, index: Int), request: ModbusRequest) => request.createTCPFrame(index)}

      val messageFactory = MessageFactory(
         indexedReadCoilsRequests,
         indexedReadDiscreteInputRequests,
         indexedReadHoldingRegisterRequests,
         indexedReadInputRegisterRequests,
         deviceConfig
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

