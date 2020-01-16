package be.cetic.tsorage.processor.update

import be.cetic.tsorage.common.messaging.ShardedTagUpdate

/**
 * A message representing a tagset used by a time series during a given shard.
 */
case class ShardedTagsetUpdate(metric: String, tagset: Map[String, String], shard: String)
{
   def splitByTag: Iterable[ShardedTagUpdate] = tagset.map(entry => ShardedTagUpdate(metric, shard, entry._1, entry._2, tagset))
}
