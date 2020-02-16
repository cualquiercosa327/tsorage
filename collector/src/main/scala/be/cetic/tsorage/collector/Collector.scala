package be.cetic.tsorage.collector

import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.ReadResult
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RestartFlow, RestartSink, RestartSource, RunnableGraph, Sink, Source, Zip, ZipN}
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.util.ByteString
import akka.{Done, NotUsed}

import be.cetic.tsorage.collector.sink.{HttpSender, MQTTSender, StdoutSender}
import be.cetic.tsorage.common.messaging.{Message, RandomMessageIterator}
import com.typesafe.config.{Config, ConfigFactory}
import GraphDSL.Implicits._
import be.cetic.tsorage.collector.source.{ModbusRTUSerialSource, ModbusRTUTCPSource, ModbusTCPSource, RandomPollSource}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import collection.JavaConverters._

/**
 * The entry point for collecting time series data points and sending them to the ingestion layer.
 */
object Collector
{
   implicit val system = ActorSystem()
   implicit val mat = ActorMaterializer()
   implicit val ec: ExecutionContextExecutor = system.dispatcher

   def main(args: Array[String]): Unit =
   {
      val conf = ConfigFactory.load("modbus_tcp_poll.conf")
      val sinks_conf = conf.getConfigList("sinks").asScala.toList
      val sources_conf = conf.getConfigList("sources").asScala.toList

      val bufferConf = conf.getConfig("buffer")

      val globalMessageSource = buildGlobalSourceGraph(sources_conf)

      val bufferSinkGenerator = () => AMQPFactory.buildAMQPSink(bufferConf)

      val bufferFlow = buildBufferGraph(Source.fromGraph(globalMessageSource), bufferSinkGenerator)
      val bufferGraph = RunnableGraph.fromGraph(bufferFlow).named("Collector Buffer Flow")
      bufferGraph.run()

      // ===

      val bufferSourceGenerator = () => AMQPFactory.buildAMQPSource(bufferConf)
      val submitterGenerator = () => buildSubmissionFlow(sinks_conf)

      val submissionSource: Source[CommittableReadResult, NotUsed] = RestartSource.withBackoff(
         minBackoff = 1 seconds,
         maxBackoff = 1 minute,
         randomFactor = 0.1
      ){ () => buildSubmissionSource(bufferSourceGenerator, submitterGenerator)}

      val submissionGraph = submissionSource.to(Sink.ignore)
      submissionGraph.run()
   }

   private def buildSubmissionFlow(submissionConfigs: List[Config]) = Flow.fromGraph(buildSubmissionGraph(submissionConfigs))

   private def buildSubmissionGraph(submissionConfigs: List[Config]) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         if(submissionConfigs isEmpty)
         {
            val flow = builder.add(Flow[CommittableReadResult].map(x => x))
            FlowShape(flow.in, flow.out)
         }
         else
         {
            val broadcast = builder.add(Broadcast[CommittableReadResult](submissionConfigs size))
            val zip = builder.add(
               ZipN[CommittableReadResult](submissionConfigs size)
            )

            val flows = submissionConfigs.map(config => {
               config.getString("type") match {
                  case "stdout/json" => StdoutSender.buildSender(config)
                  case "mqtt/pb" => MQTTSender.buildSender(config)
                  case "mqtt/json" => MQTTSender.buildSender(config)
                  case "http" => HttpSender.buildSender(config)
               }
            })

            flows.foreach(flow => {
               broadcast ~> flow ~> zip
            })

            val simplify = builder.add(
               Flow[Seq[CommittableReadResult]].map(seq => seq.head)
            )

            val ack = builder.add(
                  Flow[CommittableReadResult].map(crr => {
                  crr.ack()
                  crr
               })
            )

            zip ~> simplify ~> ack

            FlowShape(broadcast.in, ack.out)
         }
   }

   private def buildGlobalSourceGraph(sourceConfigs: List[Config]) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

      if(sourceConfigs isEmpty) {
         val s = builder.add(Source.empty)
         SourceShape(s.out)
      }
      else
      {
         val merge = builder.add(Merge[Message](sourceConfigs size))

         sourceConfigs.foreach(config => {
            val source = config.getString("type") match {
               case "random" => builder.add(Source.fromGraph(new RandomPollSource(config).buildPoller()))
               case "poll/modbus/tcp" => builder.add(Source.fromGraph(new ModbusTCPSource(config).buildPoller()))
               case "poll/modbus/rtu-tcp" => builder.add(Source.fromGraph(new ModbusRTUTCPSource(config).buildPoller()))
               case "poll/modbus/rtu" => builder.add(Source.fromGraph(new ModbusRTUSerialSource(config).buildPoller()))
            }

            source ~> merge
         })

         SourceShape(merge.out)
      }
   }

   private def buildBufferGraph(
                                  messageSource: Source[Message, _],
                                  bufferSinkGenerator: () => Sink[ByteString, Future[Done]]
                               )
                               (implicit context: ExecutionContextExecutor) = GraphDSL.create(){
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val bufferSink = RestartSink.withBackoff(
            minBackoff = 1 seconds,
            maxBackoff = 30 seconds,
            randomFactor = 0.1
         ) { (bufferSinkGenerator) }

         val msg2Bytes = builder.add(
            Flow[Message]
               .map(msg => ByteString(Message.asPB(msg).toByteArray))
         )

         messageSource ~> msg2Bytes ~> bufferSink

         ClosedShape
   }

   private def buildSubmissionSource(
                                       bufferSourceGenerator: () => Source[CommittableReadResult, NotUsed],
                                       submitterGenerator: () => Flow[CommittableReadResult, CommittableReadResult, NotUsed]
                                    ) =
      Source.fromGraph(buildSubmissionSourceGraph(bufferSourceGenerator, submitterGenerator))

   private def buildSubmissionSourceGraph(
                                       bufferSourceGenerator: () => Source[CommittableReadResult, NotUsed],
                                       submitterGenerator: () => Flow[CommittableReadResult, CommittableReadResult, _])
                                   (implicit context: ExecutionContextExecutor) = GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val source = builder.add(bufferSourceGenerator())
         val submitter = builder.add(submitterGenerator())

         source ~> submitter

         SourceShape(submitter.out)
   }
}
