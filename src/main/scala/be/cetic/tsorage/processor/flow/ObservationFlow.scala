package be.cetic.tsorage.processor.flow

import be.cetic.tsorage.processor.{Message, RawUpdate}

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
