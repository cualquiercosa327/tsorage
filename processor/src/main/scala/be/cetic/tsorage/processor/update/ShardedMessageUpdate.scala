package be.cetic.tsorage.processor.update

/**
 * A raw update, simplified in order to only show
 * informations relevant for managing sharded dynamic tag indices,
 * according to a specific sharder.
 */
case class ShardedMessageUpdate(metric: String, shard: String, tagset: Map[String, String])
