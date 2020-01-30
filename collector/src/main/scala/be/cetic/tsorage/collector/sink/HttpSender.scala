package be.cetic.tsorage.collector.sink

import akka.NotUsed
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.collector.MessageSender
import be.cetic.tsorage.common.messaging.Message
import com.google.protobuf.util.message.MessagePB
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

/**
 * Sink for sending data points to the HTTP component of the ingestion layer.
 */
object HttpSender extends MessageSender
{
   private def process(crr: CommittableReadResult)(implicit ec: ExecutionContext): Future[CommittableReadResult] =
   {
      val decoded: Message = Message.fromPB(MessagePB.parseFrom(crr.message.bytes.toArray))
      println(s"Received ${MessagePB.parseFrom(crr.message.bytes.toArray)}")
      println(s"Simulate sending ${decoded}")
      Future(crr)
   }

   /**
    * Creates a flow consisting in sending committable read result, and
    * forwarding them as an output. The result must is emitted only once the sender
    * knows it has been taken into account.
    */
   def buildSender(config: Config)(implicit ec: ExecutionContext)
   : Flow[CommittableReadResult, CommittableReadResult, NotUsed] =
   {
      Flow[CommittableReadResult].mapAsyncUnordered(4)(process)
   }
}
