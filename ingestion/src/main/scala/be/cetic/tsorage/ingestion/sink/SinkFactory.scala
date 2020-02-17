package be.cetic.tsorage.ingestion.sink

import akka.stream.scaladsl.Sink
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config

trait SinkFactory
{
   def createSink(config: Config): Sink[Message, _]
}
