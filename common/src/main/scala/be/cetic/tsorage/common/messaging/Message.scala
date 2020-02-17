package be.cetic.tsorage.common.messaging


import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

import be.cetic.tsorage.common.DateTimeConverter
import be.cetic.tsorage.common.sharder.Sharder
import com.google.protobuf.util.message.MessagePB
import com.google.protobuf.util.message.MessagePB.Value
import spray.json.JsValue
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import spray.json._

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

object Message
{
  def asPB(msg: Message): MessagePB = MessagePB(
    msg.metric,
    msg.tagset,
    msg.`type`,
    msg.values.map(value =>
      Value(
        Some(
          Timestamp.of(DateTimeConverter.localDateTimeToEpochMilli(value._1) / 1000,
          (1000000 * (DateTimeConverter.localDateTimeToEpochMilli(value._1) % 1000)).toInt)
        ),
        ByteString.copyFrom(value._2.compactPrint.getBytes("utf-8"))  // TODO : maybe a more efficient way to encode the json value?
      )
    )
  )

  def fromPB(msg: MessagePB): Message =
  {
    val values = msg.values.map(value => {
      val ts = value.datetime.get
      val millis = 1000*ts.seconds + (ts.nanos/1000000)

      val ldt = DateTimeConverter.epochMilliToUTCLocalDateTime(millis)

      val payload: JsValue = new String(value.payload.toByteArray).parseJson

      (ldt, payload)
    }).toList

    Message(
      msg.metric,
      msg.tagset,
      msg.`type`,
      values
    )
  }
}