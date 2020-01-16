package be.cetic.tsorage.common.messaging

/**
 * An update, associated with a shard, simplified in order to only show
 * informations relevant for managing sharded dynamic tag indices,
 * according to a specific sharder.
 */
case class ShardedInformationVector(metric: String, shard: String, tagset: Map[String, String])
