package be.cetic.tsorage.collector.source

import akka.stream.scaladsl.Source
import be.cetic.tsorage.collector.AMQPFactory
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.common.messaging.message.MessagePB
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor
import spray.json._

/**
 * A source that acts as an AMQP client for collecting messages.
 */
object AMQPSource extends MessageJsonSupport
{
   def createSource(config: Config)(implicit ec: ExecutionContextExecutor): Source[Message, _] =
   {
      AMQPFactory.buildAMQPSource(config)
         .map(crr => {
            crr.ack()
            val bytes = crr.message.bytes

            config.getString("type") match {
               case t if t endsWith "/pb" => Message.fromPB(MessagePB.parseFrom(bytes.toArray))
               case t if t endsWith "/json" => bytes.utf8String.parseJson.convertTo[Message]
            }
         })
   }
}
