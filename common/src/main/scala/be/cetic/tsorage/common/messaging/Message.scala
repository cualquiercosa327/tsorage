package be.cetic.tsorage.common.messaging


import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

import be.cetic.tsorage.common.DateTimeConverter
import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.common.messaging.message.MessagePB
import be.cetic.tsorage.common.messaging.message.MessagePB.Value
import spray.json.JsValue
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import spray.json._
import java.nio.ByteBuffer


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
  private def double2Bytes(number: Double) =
  {
    val byteBuffer = ByteBuffer.allocate(8)
    byteBuffer.putDouble(number)
    byteBuffer.array
  }

  private def bytes2Double(bytes: Array[Byte])=
  {
    val byteBuffer = ByteBuffer.allocate(8)
    byteBuffer.put(bytes)
    byteBuffer.flip
    byteBuffer.getDouble
  }

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
        msg.`type` match
        {

          case "tdouble" => ByteString.copyFrom(value._2 match
          {
            case JsNumber(v) => double2Bytes(v.doubleValue)
            case _ => throw new IllegalArgumentException()
          })

          case "tlong" => ByteString.copyFrom(BigInt(value._2 match
          {
            case JsNumber(v) => v.longValue
            case _ => throw new IllegalArgumentException()
          }).toByteArray)

          case _ => ByteString.copyFrom(value._2.compactPrint.getBytes("utf-8"))
        }
      )
    )
  )


  def fromPB(msg: MessagePB): Message =
  {
    val values = msg.values.map(value => {
      val ts = value.datetime.get
      val millis = 1000*ts.seconds + (ts.nanos/1000000)

      val ldt = DateTimeConverter.epochMilliToUTCLocalDateTime(millis)
      val bytes = value.payload.toByteArray

      val payload: JsValue = msg.`type` match {
        case "tdouble" => JsNumber(bytes2Double(bytes))
        case "tlong" => JsNumber(BigInt(bytes).toLong)
        case _ => new String(bytes).parseJson
      }

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