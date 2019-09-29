package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.processor.aggregator.data.SupportedValue
import be.cetic.tsorage.processor.{Message, Observation, RawUpdate}

object ObservationFlow
{
  def messageToRawUpdates(msg: Message): List[RawUpdate] = msg.values.map(
        obs => RawUpdate(
           msg.metric,
           msg.tagset,
           obs._1,
           msg.`type`,
           obs._2
        )
     )
}
