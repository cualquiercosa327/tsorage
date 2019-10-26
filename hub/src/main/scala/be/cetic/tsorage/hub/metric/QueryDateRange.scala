package be.cetic.tsorage.hub.metric

import java.time.LocalDateTime

import be.cetic.tsorage.common.json.MessageJsonSupport

/**
 * A range time in a query.
 */
sealed abstract class QueryDateRange(val `type`: String)
{
   def start: LocalDateTime
   def end: LocalDateTime
}

case class AbsoluteQueryDateRange(start: LocalDateTime, end: LocalDateTime) extends QueryDateRange("absolute")

case class RelativeQueryDateRange(duration: Long, unit: String) extends QueryDateRange("relative")
{
   def start: LocalDateTime = ???
   def end: LocalDateTime = ???
}


object QueryDateRange extends MessageJsonSupport
{
   implicit val absoluteFormat = jsonFormat2(AbsoluteQueryDateRange)
   implicit val relativeFormat = jsonFormat2(RelativeQueryDateRange)
   implicit val format = jsonFormat2(QueryDateRange.apply)

   def apply(start: LocalDateTime, end: LocalDateTime) = AbsoluteQueryDateRange(start, end)

   def unapply(qdr: QueryDateRange): Option[(LocalDateTime, LocalDateTime)] = Some((qdr.start, qdr.end))
}