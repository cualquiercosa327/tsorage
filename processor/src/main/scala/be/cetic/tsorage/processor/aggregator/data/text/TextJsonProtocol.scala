package be.cetic.tsorage.processor.aggregator.data.text

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait TextJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport
{
   implicit val textFormat = jsonFormat1(Text)
}