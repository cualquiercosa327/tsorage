package be.cetic.tsorage.ingestion.source

import java.time.{LocalDateTime, ZoneId}

import akka.NotUsed
import akka.stream.scaladsl.Source
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import spray.json.JsNumber

import collection.JavaConverters._
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random
import scala.concurrent.duration._

object RandomSourceFactory extends SourceFactory
{
   def createSource(config: Config): Source[Message, NotUsed] =
   {
      val metrics = config.getStringList("metric").asScala.toList

      val tagset = config
         .getObject("tagset")
         .keySet.toArray
         .map(key => key.toString -> config.getConfig("tagset").getString(key.toString) )
         .toMap

      val duration = Duration(config.getString("interval")) match {
         case fd: FiniteDuration => fd
         case _ =>  1 minute
      }

      Source.fromIterator(() => Generator(metrics, tagset))
         .throttle(1, duration)
   }
}

private case class Generator(metrics: List[String], tagset: Map[String, String]) extends Iterator[Message]
{
   override def hasNext: Boolean = true

   override def next(): Message = Message(
      metrics(Random.nextInt(metrics.size)),
      tagset,
      "tdouble",
      List( (LocalDateTime.now(ZoneId.of("UTC")), JsNumber(Random.nextDouble())))
   )
}
