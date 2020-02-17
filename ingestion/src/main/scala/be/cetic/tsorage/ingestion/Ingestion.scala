package be.cetic.tsorage.ingestion

import akka.{Done, NotUsed}
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, Graph, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RestartSink, RestartSource, RunnableGraph, Sink, Source, ZipN}
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.ingestion.source.{MQTTSourceFactory, RandomSourceFactory}
import com.typesafe.config.{Config, ConfigFactory}

import collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import GraphDSL.Implicits._
import akka.actor.ActorSystem
import akka.util.ByteString
import be.cetic.tsorage.ingestion.sink.StdoutSinkFactory



object Ingestion
{
   implicit val system = ActorSystem()
   implicit val mat = ActorMaterializer()
   implicit val ec: ExecutionContextExecutor = system.dispatcher

   def main(args: Array[String]): Unit =
   {


      val conf = ConfigFactory.load("ingest.conf")
      val sinks_conf = conf.getConfigList("sinks").asScala.toList
      val sources_conf = conf.getConfigList("sources").asScala.toList

      val sourceGraph = createSourceGraph(sources_conf)
      val sinkGraph = createSinkGraph(sinks_conf)

      val g = RunnableGraph.fromGraph(createGlobalGraph(sourceGraph, sinkGraph))
            .named("Ingestion Graph")

      g.run()
   }

   private def createGlobalGraph(
                           sourceGraph: Graph[SourceShape[Message], NotUsed],
                           sinkGraph: Graph[SinkShape[Message], NotUsed]
                        ) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>
         val source = builder.add(sourceGraph)
         val sink = builder.add(sinkGraph)

         source ~> sink

         ClosedShape
   }

   private def createSource(sourceConfigs: List[Config]): Source[Message, NotUsed] =
      Source.fromGraph(createSourceGraph(sourceConfigs))

   private def createSourceGraph(sourceConfigs: List[Config]): Graph[SourceShape[Message], NotUsed] = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         if(sourceConfigs.isEmpty)
         {
            val source = builder.add(Source.empty[Message])
            SourceShape(source.out)
         }
         else
         {
            val merge = builder.add(
               Merge[Message](sourceConfigs.size)
            )

            val sources = sourceConfigs.map(config => {

               val factory = config.getString("type") match {
                  case "random" => RandomSourceFactory
                  case "mqtt/pb" => MQTTSourceFactory
                  case "mqtt/json" => MQTTSourceFactory
               }

               RestartSource.withBackoff(
                  minBackoff = 1 seconds,
                  maxBackoff = 1 minute,
                  randomFactor = 0.1
               ){ () => factory.createSource(config)}
            })

            sources.foreach(source => {
               val s = builder.add(source)

               s ~> merge
            })

            SourceShape(merge.out)
         }
   }

   private def createSinkGraph(sinkConfigs: List[Config]) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         if(sinkConfigs isEmpty)
         {
            val sink = builder.add(Sink.ignore)
            SinkShape(sink.in)
         }
         else
         {
            val broadcast = builder.add(Broadcast[Message](sinkConfigs.size))

            val sinks = sinkConfigs.map(config => {
               val factory = config.getString("type") match {
                  case "stdout/json" => StdoutSinkFactory
               }

               RestartSink.withBackoff(
                  minBackoff = 1 seconds,
                  maxBackoff = 1 minute,
                  randomFactor = 0.1
               ){ () => factory.createSink(config)}
            })

            sinks.foreach(sink => {
               broadcast ~> sink
            })

            SinkShape(broadcast.in)
         }
   }
}
