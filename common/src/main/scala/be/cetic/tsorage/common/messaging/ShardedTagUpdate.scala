package be.cetic.tsorage.common.messaging

case class ShardedTagUpdate(metric: String, shard: String, tagname: String, tagvalue: String, tagset: Map[String, String])
