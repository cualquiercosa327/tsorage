//package scalapb
package be.cetic.tsorage.common.codec
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time._
import java.time.temporal.ChronoField
import java.time
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import com.google.protobuf.timestamp._
import akka.http.javadsl.model.DateTime
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.io.Source
import com.google.protobuf.timestamp._
import be.cetic.tsorage.common.messaging.Message
import spray.json.JsonParser
import sun.awt.SunGraphicsCallback.PrintHeavyweightComponentsCallback
//import akka.io.Tcp.Message
import be.cetic.tsorage.common.messaging
import spray.json.JsValue
import com.google.protobuf.Any._
//import scalapb.json4s.JsonFormat
import com.google.protobuf.util.raw2.{Raw2Proto, SENSOR2}
import com.google.protobuf.{Duration => GpbDuration, Timestamp => GpbTimestamp, _}
import scala.collection.JavaConverters._

/*
case class Message (
                     metric: String,
                     tagset: Map[String, String],
                     `type`: String,
                     values: List[(String, Double)]
                   ) extends Serializable

 */
class Codec  {

 // var msg = new messaging.Message()

  def encode (msg: Message) :Array[Byte] = {
    //var valDate :String = ""
  // var valMeasure:com.google.protobuf.Any = null
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




