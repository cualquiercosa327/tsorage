package be.cetic.tsorage.hub.tag

import be.cetic.tsorage.hub.filter.{AbsoluteQueryDateRange, Filter, FilterJsonProtocol, QueryDateRange}
import be.cetic.tsorage.hub.metric.MetricSearchQuery.jsonFormat2
import spray.json.DefaultJsonProtocol

/**
 * A representation of a query for tag values.
 *
 * @param filter The condition with which the selected metrics must correspond.
 * @param tagname The name of the extra tag, for which relevant values must be retrieved.
 * @param range The time range limiting the relevancy of the filter and the values.
 */
case class TagValueQuery(filter: Option[Filter], tagname: String, range: Option[AbsoluteQueryDateRange])

object TagValueQuery extends DefaultJsonProtocol with FilterJsonProtocol
{
   implicit val queryFormat = QueryDateRange.absoluteFormat
   implicit val format = jsonFormat3(TagValueQuery.apply)
}