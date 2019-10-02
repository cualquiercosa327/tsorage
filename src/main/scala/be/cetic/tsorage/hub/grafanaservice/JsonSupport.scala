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
final case class SearchResponse(targets: Seq[String])

/**
 * A target.
 *
 * It is used by the query request.
 *
 */
final case class Target(target: Option[String])

/**
 * A range of time in ISO 8601 date format.
 *
 * It is used by the query requests.
 *
 */
final case class TimeRange(from: String, to: String)

/**
 * A request for the query route ("/query").
 *
 */
final case class QueryRequest(targets: Seq[Target], range: TimeRange,
                              intervalMs: Long, maxDataPoints: Int)

/**
 * Data points for a single target.
 *
 * It is used by the query responses.
 *
 */
final case class DataPoints(target: String, datapoints: Seq[(BigDecimal, Long)])

/**
 * A response for the query route ("/query").
 *
 */
final case class QueryResponse(dataPointsSeq: Seq[DataPoints])

/**
 * An annotation.
 *
 * It is used by the annotation requests and responses.
 *
 */
final case class Annotation(name: String, enable: Boolean, datasource: String, iconColor: Option[String],
                            query: Option[String])

/**
 * A request for the annotation route ("/annotations").
 *
 */
final case class AnnotationRequest(annotation: Annotation)

/**
 * An annotation object (that is, an annotation with a title and a time).
 *
 * It is used by the annotation responses.
 *
 */
final case class AnnotationObject(annotation: Annotation, title: String, time: Long)

/**
 * A response for the annotation route ("/annotations").
 *
 */
final case class AnnotationResponse(annotations: Seq[AnnotationObject])

/**
 * Add the JSON support for the Grafana messages.
 *
 * More specifically , this trait contains all formats of Grafana requests and responses.
 *
 */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  // Formats for search requests.
  implicit val searchRequestFormat: RootJsonFormat[SearchRequest] = jsonFormat1(SearchRequest)

  // Formats for search responses.
  implicit object searchResponseFormat extends RootJsonFormat[SearchResponse] {
    def read(value: JsValue) = SearchResponse(value.convertTo[Seq[String]])

    def write(response: SearchResponse): JsValue = response.targets.toJson
  }

  // Formats for query requests.
  implicit val timeRangeFormat: RootJsonFormat[TimeRange] = jsonFormat2(TimeRange)
  implicit val targetFormat: RootJsonFormat[Target] = jsonFormat1(Target)
  implicit val queryRequestFormat: RootJsonFormat[QueryRequest] = jsonFormat4(QueryRequest)

  // Formats for query responses.
  implicit val dataPointsFormat: RootJsonFormat[DataPoints] = jsonFormat2(DataPoints)

  implicit object queryResponseFormat extends RootJsonFormat[QueryResponse] {
    def read(value: JsValue) = QueryResponse(value.convertTo[Seq[DataPoints]])

    def write(response: QueryResponse): JsValue = response.dataPointsSeq.toJson
  }

  // Formats for annotation requests.
  implicit val annotationFormat: RootJsonFormat[Annotation] = jsonFormat5(Annotation)
  implicit val annotationRequestFormat: RootJsonFormat[AnnotationRequest] = jsonFormat1(AnnotationRequest)

  // Formats for annotation responses.
  implicit val annotationObjectFormat: RootJsonFormat[AnnotationObject] = jsonFormat3(AnnotationObject)

  implicit object annotationResponseFormat extends RootJsonFormat[AnnotationResponse] {
    def read(value: JsValue) = AnnotationResponse(value.convertTo[Seq[AnnotationObject]])

    def write(response: AnnotationResponse): JsValue = response.annotations.toJson
  }

}
