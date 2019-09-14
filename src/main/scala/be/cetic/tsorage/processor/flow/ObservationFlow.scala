package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.processor.{FloatMessage, FloatObservation}

class ObservationFlow() {
  val fanOutObservations: FloatMessage => List[FloatObservation] = { message => message.values.map(v => FloatObservation(message.metric, message.tagset, v._1, v._2)) }
}

object ObservationFlow
{
  val flattenMessage = Flow[FloatMessage]
     .mapConcat(message => message.values.map(v => FloatObservation(message.metric, message.tagset, v._1, v._2)))
     .named("fannedOutMessages")
}
