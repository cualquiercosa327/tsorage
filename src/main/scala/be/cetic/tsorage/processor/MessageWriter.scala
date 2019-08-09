package be.cetic.tsorage.processor

import com.datastax.oss.driver.api.core.CqlSession

/**
  * An actor who writes events to the database.
  */
case class MessageWriter(session: CqlSession)
{

   def process[T](message: FloatMessage) = {

   }
}
