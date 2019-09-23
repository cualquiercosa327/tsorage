package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.processor.{Message, Observation}

class ObservationFlow() {
  def fanOutObservations[T]: Message[T] => List[Observation[T]] = { message => message.values.map(v => Observation(message.metric, message.tagset, v._1, v._2, message.support)) }
}

object ObservationFlow
{
  def flattenMessage[T] = Flow[Message[T]]
     .mapConcat(message => message.values.map(v => Observation(message.metric, message.tagset, v._1, v._2, message.support)))
     .named("fannedOutMessages")
}
