package be.cetic.tsorage.ingestion.sink

import be.cetic.tsorage.ingestion.message.PreparedFloatMessage

/**
 * A sink is a way to transmit messages to the rest of the system.
 */
trait Sink
{
   def submit(message: PreparedFloatMessage): Unit
}
