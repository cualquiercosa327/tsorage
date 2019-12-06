package be.cetic.tsorage.hub.filter

import spray.json._


trait TimeSeriesQueryJsonProtocol extends TimeSeriesJsonProtocol
   with FilterJsonProtocol
{
   implicit object TimeSeriesQueryFormat extends RootJsonFormat[TimeSeriesQuery]
   {
      implicit val queryFormat = QueryDateRange.absoluteFormat

      override def write(query: TimeSeriesQuery): JsValue = new JsObject(Map(
         "metric" -> query.metric.toJson,
         "filter" -> query.filter.toJson,
         "range" -> query.timeRange.map(_.toAbsoluteQueryDateRange).toJson
      ))

      override def read(json: JsValue): TimeSeriesQuery = {
         val fields = json.asJsObject.fields

         TimeSeriesQuery(
            fields.get("metric").map(_.convertTo[String]),
            fields("filter").convertTo[Filter],
            fields.get("range").map(_.convertTo[AbsoluteQueryDateRange])
         )
      }
   }
}
