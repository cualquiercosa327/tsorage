package be.cetic.tsorage.ingestion.sink

import be.cetic.tsorage.ingestion.message.PreparedFloatMessage

/**
 * A sink that simply displays submitted messages.
 */
object MockupSink extends Sink
{
   def submit(message: PreparedFloatMessage) = println(s"Received ${message}")
}
