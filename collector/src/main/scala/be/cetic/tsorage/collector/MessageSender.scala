package be.cetic.tsorage.collector

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Flow
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

/**
 * A generic trait for sending bufferized messages to a module of the ingestion layer.
 */
trait MessageSender
{
   /**
    * Builds a flow that sends bufferized messages represented as committable read results.
    * @param config The message to send.
    * @return A flow of bufferized messages.
    */
   def buildSender(config: Config)(implicit ec: ExecutionContext, system: ActorSystem): Flow[CommittableReadResult, CommittableReadResult, NotUsed]
}
