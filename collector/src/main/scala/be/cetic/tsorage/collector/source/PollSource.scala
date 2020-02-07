package be.cetic.tsorage.collector.source

import akka.NotUsed
import akka.stream.{OverflowStrategy, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RestartFlow, RestartSource, Source}
import be.cetic.tsorage.common.messaging.Message
import GraphDSL.Implicits._
import akka.actor.ActorSystem

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
 * A message source based on regularly polling a remote source.
 *
 * While the duration between two successive poll rounds is specified, the actual inter-round duration varies
 * depending on unpredictable circumstances.
 *
 * If the next round is supposed to start while the previous one is not finished, the next round will actually start
 * right after the previous one ends.
 *
 * At any time, at most one round is delayed. If the delay becomes so important that two rounds or more are awaiting
 * to start, the oldest delayed rounds are cancelled.
 *
 * @param interval  The expected duration between two successive poll rounds.
 *
 */
abstract class PollSource(val interval: FiniteDuration)
{
   /**
    * Generates a flow that reacts to incoming ticks by producing new messages.
    * @return  A flow that can produce new messages every time a tick is incoming.
    */
   protected def buildPollFlow()
   (implicit ec: ExecutionContextExecutor, system: ActorSystem): Flow[String, Message, NotUsed]

   def buildPoller()
   (implicit context: ExecutionContextExecutor, system: ActorSystem) = GraphDSL.create(){
      implicit builder: GraphDSL.Builder[NotUsed] =>

      val tick = Source
         .tick(0 second, interval, "tick")
         .buffer(1, OverflowStrategy.dropHead)

      val poll = builder.add(
            RestartFlow.withBackoff(
            minBackoff = 1 second,
            maxBackoff = 1 minute,
            randomFactor = 0.1
         ) { (buildPollFlow) }
      )

      tick ~> poll

      SourceShape(poll.out)
   }
}
