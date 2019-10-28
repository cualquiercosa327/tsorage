package be.cetic.tsorage.hub.metric

import be.cetic.tsorage.hub.filter.{AbsoluteQueryDateRange, Filter, FilterJsonProtocol, QueryDateRange}
import spray.json.DefaultJsonProtocol

/**
 * A representation of a metric search.
 */
case class MetricSearchQuery(filter: Option[Filter], range: Option[AbsoluteQueryDateRange])


object MetricSearchQuery extends DefaultJsonProtocol with FilterJsonProtocol
{
   implicit val queryFormat = QueryDateRange.absoluteFormat
   implicit val format = jsonFormat2(MetricSearchQuery.apply)
}