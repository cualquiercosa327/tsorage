package be.cetic.tsorage.processor.update

case class ShardedTagUpdate(metric: String, shard: String, tagname: String, tagvalue: String, tagset: Map[String, String])

