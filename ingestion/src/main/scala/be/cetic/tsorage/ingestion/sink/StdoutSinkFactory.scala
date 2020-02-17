package be.cetic.tsorage.ingestion.sink

import akka.NotUsed
import akka.stream.scaladsl.Sink
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config

import spray.json._

object StdoutSinkFactory extends SinkFactory with MessageJsonSupport
{
   def createSink(config: Config): Sink[Message, _] =
   {
      Sink.foreach[Message](msg => println(msg.toJson))
   }
}

