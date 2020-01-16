package be.cetic.tsorage.common.messaging


import java.time.LocalDateTime

import be.cetic.tsorage.common.sharder.Sharder
import spray.json.JsValue

/**
 * A package message containing observations.
 */
case class Message(
                     metric: String,
                     tagset: Map[String, String],
                     `type`: String,
                     values: List[(LocalDateTime, JsValue)]
                  ) extends InformationVector with Serializable
{
  override def splitByShard(sharder: Sharder) = {
    val shards = values.map(value => sharder.shard(value._1)).toSet
    shards.map(shard => ShardedInformationVector(metric, shard, tagset))
  }

  override def splitByTagAndShard(sharder: Sharder) = {
    val shards = values.map(value => sharder.shard(value._1)).distinct
    shards.flatMap(shard => tagset.map(tag => ShardedTagUpdate(metric, shard, tag._1, tag._2, tagset)))
  }
}