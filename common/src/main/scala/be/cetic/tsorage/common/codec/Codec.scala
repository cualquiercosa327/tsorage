//package scalapb
package be.cetic.tsorage.common.codec
import java.time.{Instant, LocalDateTime, ZoneOffset}

import be.cetic.tsorage.common.messaging.Message
import com.google.protobuf.timestamp._
import com.google.protobuf.util.raw2.SENSOR2
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, _}

class Codec  {

  def encode (msg: Message) :Array[Byte] = {

    var Val: Seq[SENSOR2.Values] = Seq.empty[SENSOR2.Values]

    for (x <- msg.values.indices) {
      val instant: Instant = msg.values(x)._1.toInstant(ZoneOffset.UTC)
      val valDate = Timestamp.of(instant.getEpochSecond, instant.getNano)
      val z = msg.values(x)._2.convertTo[Double]


      val values = SENSOR2.Values().withDateTime(valDate).withMeasure(z)//.withMeasure(com.google.protobuf.any.Any(valMeasure))
     Val = Val :+ values
    }
    val encodedMsg:Array[Byte] = SENSOR2()
      .withMetric(msg.metric)
      .withTagset(msg.tagset)
      .withType(msg.`type`)
      .withV(Val)
      .toByteArray

    encodedMsg//.toByteArray

  }

  def decode(array: Array[Byte]): Message = {

    var Val: List[(LocalDateTime, JsValue)] = List.empty

    val metric: String = SENSOR2.parseFrom(array).metric.toString

    val tagset: Map[String, String] = SENSOR2.parseFrom(array).tagset

    val `type`: String =  SENSOR2.parseFrom(array).`type`.toString

    for (x <- SENSOR2.parseFrom(array).v.indices) {
      val v = SENSOR2.parseFrom(array).v(x)

      //val d = LocalDateTime.from(v.dateTime.asInstanceOf[TimestampProto)
      //println("mmmmmmmmmmmmmmmmm" + d)

      val valDate:LocalDateTime = v.dateTime match{
      case Some(value) => LocalDateTime.ofEpochSecond(value.seconds, v.dateTime.get.nanos, ZoneOffset.UTC)
      // case None => InvalidOperationException
        }

       // val d =  LocalDateTime.ofEpochSecond(v.dateTime.get.seconds, 0, ZoneOffset.UTC).
     val valMeasure:JsValue = v.measure.toJson

      Val = Val :+ (valDate, valMeasure )

    }

    val decodedMsg:Message = new Message (metric, tagset, `type`, Val)
    decodedMsg
  }
}




