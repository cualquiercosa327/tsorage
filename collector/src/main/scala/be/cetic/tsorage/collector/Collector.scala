package be.cetic.tsorage.collector

import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.ReadResult
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, RestartFlow, RestartSink, RestartSource, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.util.ByteString
import akka.{Done, NotUsed}
import be.cetic.tsorage.collector.sink.{HttpSender, MQTTSender, StdoutSender}
import be.cetic.tsorage.common.messaging.{Message, RandomMessageIterator}
import com.typesafe.config.{Config, ConfigFactory}
import GraphDSL.Implicits._
import be.cetic.tsorage.collector.source.RandomPollSource

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
      val ingestion_conf = conf.getConfig("sink")
      val source_conf = conf.getConfigList("sources").asScala.toList

      val bufferConf = conf.getConfig("buffer")

      val globalMessageSource = buildGlobalMessageGraph(source_conf)

      val bufferSinkGenerator = () => AMQPFactory.buildAMQPSink(bufferConf)

      // ===

      val bufferSourceGenerator = () => AMQPFactory.buildAMQPSource(bufferConf)

      val submitterGenerator = () => ingestion_conf.getString("type") match {
            case "http" => HttpSender.buildSender(ingestion_conf)
            case "mqtt" => MQTTSender.buildSender(ingestion_conf)
            case "stdout/json" => StdoutSender.buildSender(ingestion_conf)
         }

      val bufferFlow = buildBufferGraph(Source.fromGraph(globalMessageSource), bufferSinkGenerator)
      val bufferGraph = RunnableGraph.fromGraph(bufferFlow).named("Collector Buffer Flow")
      bufferGraph.run()

      val submissionFlow = RestartSource.withBackoff(
         minBackoff = 5 seconds,
         maxBackoff = 1 minute,
         randomFactor = 0.1
      ){ () => Source.fromGraph(buildSubmissionSource(bufferSourceGenerator, submitterGenerator)) }.to(Sink.ignore)

      val submissionGraph = RunnableGraph.fromGraph(submissionFlow).named("Collector Submission Flow")
      submissionGraph.run()
   }

   private def buildGlobalMessageGraph(sourceConfigs: List[Config]) = GraphDSL.create()
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
                                       submitterGenerator: () => Flow[CommittableReadResult, CommittableReadResult, _])
                                   (implicit context: ExecutionContextExecutor) = GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val ack = builder.add(
               Flow[CommittableReadResult].map(crr => {
                  crr.ack()
                  crr.message
            })
         )

         bufferSourceGenerator() ~> submitterGenerator() ~> ack

         SourceShape(ack.out)
   }
}
