package be.cetic.tsorage.ingestion.sink

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import spray.json._

import scala.concurrent.ExecutionContextExecutor

object StdoutSinkFactory extends SinkFactory with MessageJsonSupport
{
   def createSink(config: Config)
                 (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContextExecutor): Sink[Message, _] =
   {
      Sink.foreach[Message](msg => println(msg.toJson))
   }
}

