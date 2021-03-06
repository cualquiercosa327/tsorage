package be.cetic.tsorage.collector.source

import java.time.{LocalDateTime, ZoneId}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import spray.json.JsNumber

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Random

/**
 * A poll source that randomly generates its messages.
 */
class RandomPollSource(val config: Config) extends PollSource(
   Duration(config.getString("interval")) match {
      case fd: FiniteDuration => fd
      case _ =>  1 minute
   }
)
{
   val metrics = config.getStringList("metric")

   val tagset = config
      .getObject("tagset")
      .keySet.toArray
      .map(key => key.toString -> config.getConfig("tagset").getString(key.toString) )
      .toMap

   override def buildPollFlow()
   (implicit ec: ExecutionContextExecutor, system: ActorSystem, am: ActorMaterializer): Flow[String, Message, NotUsed] = {
      Flow[String].map(tick => {
         Message(
            metrics.get(Random.nextInt(metrics size)),
            tagset,
            "tdouble",
            List((LocalDateTime.now(ZoneId.of("UTC")), JsNumber(Random.nextDouble())))
         )
      })
   }
}
