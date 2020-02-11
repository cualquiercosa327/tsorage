package be.cetic.tsorage.collector.source

import java.nio.ByteOrder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, OverflowStrategy, SourceShape}
import akka.stream.scaladsl.{BidiFlow, Flow, Framing, GraphDSL, Keep, RunnableGraph, Sink, Source, Tcp}
import akka.util.ByteString
import be.cetic.tsorage.collector.modbus.{Extract, ModbusFunction}
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import GraphDSL.Implicits._
import be.cetic.tsorage.collector.modbus.comm.rtu.{ModbusRTUResponse, RTUMessageFactory}
import be.cetic.tsorage.collector.modbus.comm.{ModbusFraming, ModbusRequest, ModbusResponseFactory}
import akka.pattern.after
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContextExecutor, Future, TimeoutException}
import scala.concurrent.duration._
import scala.concurrent.duration.{Duration, FiniteDuration}

import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision.resumingDecider

/**
 * A Modbus RTU over TCP source.
 *
 **/
class ModbusRTUTCPSource(val config: Config) extends PollSource(
   Duration(config.getString("interval")) match {
      case fd: FiniteDuration => fd
      case _ =>  1 minute
   }
) with LazyLogging
{
   /**
    * Generates a flow that reacts to incoming ticks by producing new messages.
    *
    * @return A flow that can produce new messages every time a tick is incoming.
    */
   override protected def buildPollFlow()
   (implicit ec: ExecutionContextExecutor, system: ActorSystem, am: ActorMaterializer): Flow[String, Message, NotUsed] =
   {
      val host: String = config.getString("host")
      val port: Int = config.getInt("port")

      val extracts = Extract.fromSourceConfig(config)

      val requests: List[ModbusRequest] = extracts.map{
         case (f: ModbusFunction, fExtracts: List[Extract]) => f -> f.prepareRequests(fExtracts)
      }.values.flatten.toList

      Flow.fromGraph(createGraph(host, port, requests, extracts))
   }

   private def createGraph(
                             host: String,
                             port: Int,
                             requests: List[ModbusRequest],
                             extracts: Map[ModbusFunction, List[Extract]]
                          )
   (implicit
    context: ExecutionContextExecutor,
    system: ActorSystem,
    am: ActorMaterializer) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val modbusFraming = ModbusFraming.rtuFraming

         val messageFactory = RTUMessageFactory(extracts)

         val incoming: Flow[ByteString, ModbusRTUResponse, _] = Flow[ByteString]
            .via(modbusFraming)
            .via(Flow.fromFunction({response: ByteString => ModbusResponseFactory.fromRTUByteString(response)}))

         val outgoing:Flow[ModbusRequest, ByteString, _] = Flow fromFunction {request: ModbusRequest => ByteString(request.createRTUFrame())}

         val protocol = BidiFlow.fromFlows(incoming, outgoing)

         val modbusFlow =
            Tcp()
               .outgoingConnection(host, port)
               .join(protocol)


         val requestFlow = builder.add(
            Flow[String]      // Converts ticks to modbus request frames
               .mapConcat(tick => requests)
         )

         val responseFlow = Flow[ModbusRequest]
            .mapAsync(1)(request => sendRequest(modbusFlow, request).map(response => (request, response)))
            .withAttributes(supervisionStrategy(resumingDecider))

         val messageFlow = builder.add(
            Flow[(ModbusRequest, ModbusRTUResponse)]
               .mapConcat(elem => messageFactory.responseToMessages(elem._1, elem._2))
               .buffer(500, OverflowStrategy.backpressure)
         )

         requestFlow ~> responseFlow ~> messageFlow

         FlowShape(requestFlow.in, messageFlow.out)
   }

   private def sendRequest(modbusFlow: Flow[ModbusRequest, ModbusRTUResponse, _], request: ModbusRequest)
                          (implicit
                           ec: ExecutionContextExecutor,
                           system: ActorSystem,
                           am: ActorMaterializer
                          ): Future[ModbusRTUResponse] =
   {
      val breakOnTimeout: Future[Nothing] = after(2 seconds, system.scheduler)(Future failed new TimeoutException())

      val response: Future[ModbusRTUResponse] =
      {
         val sink = Sink.head[ModbusRTUResponse]

         val g: RunnableGraph[Future[ModbusRTUResponse]] = RunnableGraph.fromGraph(GraphDSL.create(sink) {
            implicit builder =>
               mySink =>
                  val source = Source.single(request)

                  source ~> modbusFlow ~> mySink.in

                  ClosedShape
         })

         g.run()
      }

      Future firstCompletedOf List(
         breakOnTimeout,
         response
      )
   }
}
