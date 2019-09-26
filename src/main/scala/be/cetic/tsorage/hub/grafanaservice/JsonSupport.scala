package be.cetic.tsorage.hub.grafanaservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

/**
 * A request for the search route ("/search").
 *
 */
final case class SearchRequest(target: Option[String])

/**
 * A response for the search route ("/search").
 *
 */
final case class SearchResponse(targets: List[String])

/**
 * A target (used by the query request).
 *
 */
final case class Target(target: Option[String])

/**
 * A range of time in ISO 8601 date format (used by the query request).
 *
 */
final case class TimeRange(from: String, to: String)

/**
 * A request for the query route ("/query").
 *
 */
final case class QueryRequest(targets: List[Target], range: TimeRange,
                              intervalMs: Long, maxDataPoints: Int)

/**
 * Data points for a single target (used by the query response).
 *
 */
final case class DataPoints(target: String, datapoints: List[List[BigDecimal]])

/**
 * A response for the query route ("/query").
 *
 */
final case class QueryResponse(dataPointsList: List[DataPoints])

/**
 * Add the JSON support for the Grafana messages.
 * More specifically , this trait contains all formats of Grafana requests and responses.
 *
 */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  // Formats for search requests.
  implicit val searchRequestFormat: RootJsonFormat[SearchRequest] = jsonFormat1(SearchRequest)

  // Formats for search responses.
  implicit object searchResponseFormat extends RootJsonFormat[SearchResponse] {
    def read(value: JsValue) = SearchResponse(value.convertTo[List[String]])

    def write(response: SearchResponse): JsValue = response.targets.toJson
  }

  // Formats for query requests.
  implicit val timeRangeFormat: RootJsonFormat[TimeRange] = jsonFormat2(TimeRange)
  implicit val targetFormat: RootJsonFormat[Target] = jsonFormat1(Target)
  implicit val queryRequestFormat: RootJsonFormat[QueryRequest] = jsonFormat4(QueryRequest)

  // Formats for query responses.
  implicit val dataPointsFormat: RootJsonFormat[DataPoints] = jsonFormat2(DataPoints)

  implicit object queryResponseFormat extends RootJsonFormat[QueryResponse] {
    def read(value: JsValue) = QueryResponse(value.convertTo[List[DataPoints]])

    def write(response: QueryResponse): JsValue = response.dataPointsList.toJson
  }

}
