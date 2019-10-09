package be.cetic.tsorage.hub.grafana

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import be.cetic.tsorage.hub.grafana.jsonsupport.{GrafanaJsonSupport, SearchResponse}
import org.scalatest.{Matchers, WordSpec}

class GrafanaServiceTest extends WordSpec with Matchers with ScalatestRouteTest with GrafanaJsonSupport {
  val database: Database = FakeDatabase
  val grafanaService = new GrafanaService(database)
  val getMetricNamesRoute: Route = grafanaService.getMetricNamesRoute
  val postMetricNamesRoute: Route = grafanaService.postMetricNamesRoute
  val postQueryRoute: Route = grafanaService.postQueryRoute
  val postAnnotationRoute: Route = grafanaService.postAnnotationRoute

  "The Grafana service" should { // TODO: change name of this string?
    // Search route.
    "return the name of all metrics for GET/POST requests to the search path" in {
      Get("/search") ~> getMetricNamesRoute ~> check {
        responseAs[SearchResponse].targets.toSet shouldEqual database.metrics.toSet
      }
      Post("/search", HttpEntity(`application/json`, "{}")) ~> postMetricNamesRoute ~> check {
        responseAs[SearchResponse].targets.toSet shouldEqual database.metrics.toSet
      }
    }
  }
}
