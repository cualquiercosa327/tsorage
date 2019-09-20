package be.cetic.tsorage.processor

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

trait TimeFormatHelper
{
   private val datetimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

   /**
     * Converts a localdatetime, that implicitly represents an instant on the GMT timezone, into a Cassandra-compliant string.
     * @param ldt The local date time to convert.
     * @return A string representation of this datetime.
     */
   def formatLDT(ldt : LocalDateTime): String = ZonedDateTime.of(ldt, ZoneId.of("GMT")).format(datetimeFormatter)
}
