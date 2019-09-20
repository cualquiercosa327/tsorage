package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.processor.{Message, Observation}

class ObservationFlow() {
  val fanOutObservations: Message[Double] => List[Observation[Double]] = { message => message.values.map(v => Observation(message.metric, message.tagset, v._1, v._2)) }
}

object ObservationFlow
{
  val flattenMessage = Flow[Message[Double]]
     .mapConcat(message => message.values.map(v => Observation(message.metric, message.tagset, v._1, v._2)))
     .named("fannedOutMessages")
}
