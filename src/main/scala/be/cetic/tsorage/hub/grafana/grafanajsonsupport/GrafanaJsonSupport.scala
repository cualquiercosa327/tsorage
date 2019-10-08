package be.cetic.tsorage.hub.grafana.grafanajsonsupport

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

/**
 * Add the JSON support for the Grafana messages.
 *
 * More specifically , this trait contains all formats of Grafana requests and responses.
 *
 */
trait GrafanaJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
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
