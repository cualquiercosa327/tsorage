package be.cetic.tsorage.common.messaging

import java.time.LocalDateTime

import be.cetic.tsorage.common.sharder.Sharder
import spray.json.JsValue

/**
  * An atomic observation.
  */
case class Observation(
                         val metric: String,
                         val tagset: Map[String, String],
                         datetime: LocalDateTime,
                         value: JsValue,
                         `type`: String
                         ) extends InformationVector
{
   override def splitByShard(sharder: Sharder) = List(ShardedInformationVector(
      metric,
      sharder.shard(datetime),
      tagset
   ))

   override def splitByTagAndShard(sharder: Sharder) = tagset.map(tag => ShardedTagUpdate(
      metric,
      sharder.shard(datetime),
      tag._1,
      tag._2,
      tagset
   ))
}
