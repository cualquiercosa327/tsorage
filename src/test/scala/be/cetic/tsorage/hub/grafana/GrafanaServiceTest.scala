package be.cetic.tsorage.hub.grafana

import be.cetic.tsorage.hub.grafana.jsonsupport.{Annotation, AnnotationRequest, AnnotationResponse, GrafanaJsonSupport, SearchRequest, SearchResponse}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import spray.json._
import org.scalatest.{Matchers, MustMatchers, WordSpec}


class GrafanaServiceTest extends WordSpec with Matchers with ScalatestRouteTest with GrafanaJsonSupport {
  val database: Database = FakeDatabase
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

      val request = SearchRequest(Some("Temperature")).toJson.toString
      Post("/search", HttpEntity(`application/json`, request)) ~> postMetricNamesRoute ~> check {
        val response = responseAs[SearchResponse]

        response.targets.toSet shouldEqual database.metrics.toSet
      }
    }

    // Annotation route.
    "return an annotation for POST requests to the annotation path" in {
      val annotation = Annotation("my_annotation", enable = true, "SimpleJson", Some("rgba(255, 96, 96, 1)"), None)
      val request = AnnotationRequest(annotation).toJson.toString
      Post("/annotations", HttpEntity(`application/json`, request)) ~> postAnnotationRoute ~> check {
        val response = responseAs[AnnotationResponse]

        response.annotations.head.annotation shouldEqual annotation

        val currentTime = System.currentTimeMillis
        response.annotations.head.time shouldEqual currentTime +- 60000 // Tolerance of one minute.
      }
    }
  }
}
