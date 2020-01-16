package be.cetic.tsorage.common.messaging

import be.cetic.tsorage.common.sharder.Sharder

/**
 * An abstract entity containing some values and attached to a time series.
 */
abstract class InformationVector() extends Serializable
{
   def `type`: String

   def metric: String

   def tagset: Map[String, String]

   def splitByTag: scala.collection.immutable.Iterable[TagUpdate] = tagset.map(tag => TagUpdate(metric, tag._1, tag._2, tagset))

   def splitByShard(sharder: Sharder): scala.collection.immutable.Iterable[ShardedInformationVector]

   def splitByTagAndShard(sharder: Sharder): scala.collection.immutable.Iterable[ShardedTagUpdate]
}
