package be.cetic.tsorage.processor.aggregator.data.position2d

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait Position2DJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport
{
   implicit val position2dFormat = jsonFormat2(Position2D)
}
