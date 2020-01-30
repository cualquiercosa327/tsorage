package be.cetic.tsorage.collector.sink

import akka.NotUsed
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.collector.MessageSender
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

/**
 * Sink for sending messages to the MQTT ingestion service.
 */
object MQTTSender extends MessageSender
{
   /**
    * Builds a flow that sends bufferized messages represented as committable read results.
    *
    * @param config The message to send.
    * @return A flow of bufferized messages.
    */
   override def buildSender(config: Config)(implicit ec: ExecutionContext)
   : Flow[CommittableReadResult, CommittableReadResult, NotUsed] =
   {
      Flow[CommittableReadResult].map(crr => {
         println(crr)
         crr
      })
   }
}
