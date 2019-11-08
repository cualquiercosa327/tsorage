package be.cetic.tsorage.processor.update

/**
 * A raw update, simplified in order to only show
 * informations relevant for managing sharded dynamic tag indices,
 * according to a specific sharder.
 */
case class ShardedDynamicTagUpdate(metric: String, shard: String, tagname: String, tagvalue: String, tagset: Map[String, String])