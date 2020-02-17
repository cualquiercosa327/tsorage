package be.cetic.tsorage.ingestion.sink

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor

trait SinkFactory
{
   def createSink(config: Config)(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContextExecutor): Sink[Message, _]
}
