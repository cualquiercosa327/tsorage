package be.cetic.tsorage.hub.grafana

import java.time.Instant

import be.cetic.tsorage.hub.grafana.jsonsupport.{
  Annotation, AnnotationRequest, AnnotationResponse,
  GrafanaJsonSupport, QueryRequest, QueryResponse, SearchRequest, SearchResponse, Target, TimeRange
}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import spray.json._
import org.scalatest.{Matchers, WordSpec}

import scala.util.Try


class GrafanaServiceTest extends WordSpec with Matchers with ScalatestRouteTest with GrafanaJsonSupport {
  //val database: Database = new FakeDatabase(1568991600) // Correspond to Friday 20 September 2019 15:00:00.
  val database: Database = new FakeDatabase(1568991734) // Correspond to Friday 20 September 2019 15:02:14.
  val grafanaService = new GrafanaService(database)
  val getMetricNamesRoute: Route = grafanaService.getMetricNamesRoute
  val postMetricNamesRoute: Route = grafanaService.postMetricNamesRoute
  val postQueryRoute: Route = grafanaService.postQueryRoute
  val postAnnotationRoute: Route = grafanaService.postAnnotationRoute

  "The Grafana service" should {
    // Search route.
    "return the name of all metrics for GET/POST requests to the search path" in {
      Get("/search") ~> getMetricNamesRoute ~> check {
        val response = responseAs[SearchResponse]

        response.targets.toSet shouldEqual database.metrics.toSet
      }

      val request = SearchRequest(Some("Temperature"))
      Post("/search", HttpEntity(`application/json`, request.toJson.toString)) ~> postMetricNamesRoute ~> check {
        val response = responseAs[SearchResponse]

        response.targets.toSet shouldEqual database.metrics.toSet
      }
    }

    // Query route (returns success).
    "correctly queries the database for POST requests to the query path" in {
      val request = QueryRequest(
        Seq(Target(Some("temperature")), Target(Some("pressure"))),
        TimeRange("2019-09-20T20:20:00.000Z", "2019-09-21T03:30:00.000Z"),
        None, Some(1000)
      )
      Post("/query", HttpEntity(`application/json`, request.toJson.toString)) ~> postQueryRoute ~> check {
        val response = responseAs[QueryResponse]

        // Test the metric names.
        val metrics = response.dataPointsSeq.map(_.target) // Get the name of metrics.
        metrics.toSet shouldEqual request.targets.flatMap(_.target).toSet

        // Test data.
        val timestampFrom = Instant.parse(request.range.from).toEpochMilli
        val timestampTo = Instant.parse(request.range.to).toEpochMilli
        response.dataPointsSeq.foreach { dataPoints =>
          // Test if there are some data.
          dataPoints.datapoints.size should be > 5

          // Test if data in the response is within the correct time interval.
          dataPoints.datapoints.foreach {
            _._2 should (be >= timestampFrom and be <= timestampTo)
          }
        }

        // Test if there are at most roughly `request.maxDataPoints.get` data.
        response.dataPointsSeq.foreach {
          _.datapoints.size shouldEqual request.maxDataPoints.get +- 5 // Tolerance of five data.
        }
      }
    }

    // Query route (returns an error).
    "correctly handles input errors for POST request to the query path" in {
      // Request with a non-existent-metric.
      val request1 = QueryRequest(
        Seq(Target(Some("non-existent-metric"))),
        TimeRange("2019-09-20T20:20:00.000Z", "2019-09-21T03:30:00.000Z"),
        None, None
      )
      // Request with bad time format ("Z" is missing in this case).
      val request2 = QueryRequest(
        Seq(Target(Some("temperature")), Target(Some("pressure"))),
        TimeRange("2019-09-20T20:20:00.000", "2019-09-21T03:30:00.000Z"),
        None, None
      )
      // Request with an incorrect interval.
      val request3 = QueryRequest(
        Seq(Target(Some("pressure"))),
        TimeRange("2019-09-20T20:20:00.000Z", "2019-09-21T03:30:00.000Z"),
        Some(0), None
      )
      // Request with an incorrect maximum number of data points to keep.
      val request4 = QueryRequest(
        Seq(Target(Some("pressure"))),
        TimeRange("2019-09-20T20:20:00.000Z", "2019-09-21T03:30:00.000Z"),
        None, Some(-1)
      )
      Post("/query", HttpEntity(`application/json`, request1.toJson.toString)) ~> postQueryRoute ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Post("/query", HttpEntity(`application/json`, request2.toJson.toString)) ~> postQueryRoute ~> check {
        status shouldEqual StatusCodes.MethodNotAllowed
      }
      Post("/query", HttpEntity(`application/json`, request3.toJson.toString)) ~> postQueryRoute ~> check {
        status shouldEqual StatusCodes.MethodNotAllowed
      }
      Post("/query", HttpEntity(`application/json`, request4.toJson.toString)) ~> postQueryRoute ~> check {
        status shouldEqual StatusCodes.MethodNotAllowed
      }
    }

    // Annotation route.
    "return an annotation for POST requests to the annotation path" in {
      val annotation = Annotation("my_annotation", enable = true, "SimpleJson", Some("rgba(255, 96, 96, 1)"), None)
      val request = AnnotationRequest(annotation)
      Post("/annotations", HttpEntity(`application/json`, request.toJson.toString)) ~> postAnnotationRoute ~> check {
        val response = responseAs[AnnotationResponse]

        response.annotations.head.annotation shouldEqual annotation

        val currentTime = System.currentTimeMillis
        response.annotations.head.time shouldEqual currentTime +- 60000 // Tolerance of one minute.
      }
    }
  }
}
