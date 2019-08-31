package be.cetic.tsorage.ingestion.message

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
 * Created by Mathieu Goeminne.
 */
trait FloatMessageJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
   implicit val messageFormat = jsonFormat6(FloatMessage)
   implicit val bodyFormat = jsonFormat1(FloatBody)
}
