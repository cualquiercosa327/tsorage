package be.cetic.tsorage.ingestion.source

import akka.NotUsed
import akka.stream.scaladsl.Source
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config

trait SourceFactory
{
   def createSource(config: Config): Source[Message, _]
}
