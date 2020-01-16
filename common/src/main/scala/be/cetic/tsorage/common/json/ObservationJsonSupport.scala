package be.cetic.tsorage.common.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import be.cetic.tsorage.common.messaging.Observation
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait ObservationJsonSupport extends DefaultJsonProtocol
   with SprayJsonSupport
   with MessageJsonSupport
{
   implicit val observationJsonFormat = jsonFormat5(Observation)
}
