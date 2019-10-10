package be.cetic.tsorage.hub.grafana

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.DebuggingDirectives
import be.cetic.tsorage.hub.grafana.backend.GrafanaBackend
import be.cetic.tsorage.hub.grafana.jsonsupport.{AnnotationRequest, GrafanaJsonSupport, QueryRequest, SearchRequest}

import scala.concurrent.ExecutionContext

class GrafanaService(database: Database)(implicit executionContext: ExecutionContext) extends Directives
  with GrafanaJsonSupport {
  val grafanaRequestHandler = new GrafanaBackend(database)

  /**
   * Grafana connection test route for Grafana. It allows Grafana to test the connection with the server.
   *
   * @return the Grafana connection test route.
   */
  def grafanaConnectionTest: Route = path("v1" / "api" / "grafana") {
    get {
      DebuggingDirectives.logRequestResult("Grafana connection test route (/v1/api/grafana)", Logging.InfoLevel) {
        complete(StatusCodes.OK)
      }
    }
  }

  val grafanaConnectionTestRoute: Route = grafanaConnectionTest

  /**
   * Search route. It allows to get the name of all metrics.
   *
   * @return the search route.
   */
  def getMetricNames: Route = path("v1" / "api" / "grafana" / "search") {
    get {
      DebuggingDirectives.logRequestResult("Search route (/v1/api/grafana/search)", Logging.InfoLevel) {
        grafanaRequestHandler.handleSearchRoute(None)
      }
    }
  }

  val getMetricNamesRoute: Route = getMetricNames

  /**
   * Search route. It allows to get the name of all metrics.
   *
   * @return the search route.
   */
  def postMetricNames: Route = path("v1" / "api" / "grafana" / "search") {
    post {
      entity(as[SearchRequest]) { request =>
        DebuggingDirectives.logRequestResult("Search route (/v1/api/grafana/search)", Logging.InfoLevel) {
          grafanaRequestHandler.handleSearchRoute(Some(request))
        }
      }
    }
  }

  val postMetricNamesRoute: Route = postMetricNames

  /**
   * Query route. It allows to query the database.
   *
   * @return the query route.
   */
  def postQuery: Route = path("v1" / "api" / "grafana" / "query") {
    post {
      entity(as[QueryRequest]) { request =>
        DebuggingDirectives.logRequestResult("Query route (/v1/api/grafana/query)", Logging.InfoLevel) {
          grafanaRequestHandler.handleQueryRoute(request)
        }
      }
    }
  }

  val postQueryRoute: Route = postQuery

  /**
   * Annotation route.
   *
   * @return the annotation route.
   */
  def postAnnotation: Route = path("v1" / "api" / "grafana" / "annotations") {
    post {
      entity(as[AnnotationRequest]) { request =>
        DebuggingDirectives.logRequestResult("Annotation route (/v1/api/grafana/annotations)", Logging.InfoLevel) {
          grafanaRequestHandler.handleAnnotationRoute(request)
        }
      }
    }
  }

  val postAnnotationRoute: Route = postAnnotation

  val routes: Route = concat(grafanaConnectionTestRoute, getMetricNamesRoute, postMetricNamesRoute, postQueryRoute,
    postAnnotationRoute)
}
