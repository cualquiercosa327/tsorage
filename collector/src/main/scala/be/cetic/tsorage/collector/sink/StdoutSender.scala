package be.cetic.tsorage.collector.sink

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.collector.MessageSender
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.common.messaging.message.MessagePB
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import spray.json._

/**
 * A collector sink that sends incoming messages to the standard output.
 *
 * Message prints use the compact JSON format
 */
object StdoutSender extends MessageSender
   with MessageJsonSupport
{
   /**
    * Builds a flow that sends bufferized messages represented as committable read results.
    *
    * @param config The message to send.
    * @return A flow of bufferized messages.
    */
   override def buildSender(config: Config)(implicit ec: ExecutionContext, system: ActorSystem):
   Flow[CommittableReadResult, CommittableReadResult, NotUsed] =
   {
      Flow[CommittableReadResult].map(crr => {
         val decoded: Message = Message.fromPB(MessagePB.parseFrom(crr.message.bytes.toArray))
         System.out.println(decoded.toJson.compactPrint)

         crr
      })
   }
}
