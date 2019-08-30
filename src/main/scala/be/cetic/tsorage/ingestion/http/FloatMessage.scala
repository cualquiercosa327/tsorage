package be.cetic.tsorage.ingestion.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class FloatMessage(
                          metric: String,
                          points: List[(Double, Float)],
                          `type`: Option[String],
                          interval: Option[Long],
                          host: Option[String],
                          tags: List[String]
                       )

case class FloatBody(series: List[FloatMessage])

trait FloatMessageJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
   implicit val messageFormat = jsonFormat6(FloatMessage)
   implicit val bodyFormat = jsonFormat1(FloatBody)
}
