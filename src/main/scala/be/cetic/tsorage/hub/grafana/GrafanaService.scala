package be.cetic.tsorage.hub.grafana

import akka.event.Logging
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.DebuggingDirectives
import be.cetic.tsorage.hub.grafana.backend.GrafanaBackend
import be.cetic.tsorage.hub.grafana.jsonsupport.{AnnotationRequest, GrafanaJsonSupport, QueryRequest, SearchRequest}

import scala.concurrent.ExecutionContext

class GrafanaService(database: Database)(implicit executionContext: ExecutionContext) extends Directives
  with GrafanaJsonSupport {
  val grafanaRequestHandler = new GrafanaBackend(database)

  /**
   * Search route. It allows to get the name of all metrics.
   *
   * @return the search route.
   */
  def getMetricNames: Route = path("search") {
    get {
      DebuggingDirectives.logRequestResult("Search route (/search)", Logging.InfoLevel) {
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
  def postMetricNames: Route = path("search") {
    post {
      entity(as[SearchRequest]) { request =>
        DebuggingDirectives.logRequestResult("Search route (/search)", Logging.InfoLevel) {
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
  def postQuery: Route = path("query") {
    post {
      entity(as[QueryRequest]) { request =>
        DebuggingDirectives.logRequestResult("Query route (/query)", Logging.InfoLevel) {
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
  def postAnnotation: Route = path("annotations") {
    post {
      entity(as[AnnotationRequest]) { request =>
        DebuggingDirectives.logRequestResult("Annotation route (/annotations)", Logging.InfoLevel) {
          grafanaRequestHandler.handleAnnotationRoute(request)
        }
      }
    }
  }

  val postAnnotationRoute: Route = postAnnotation

  val routes: Route = concat(getMetricNamesRoute, postMetricNamesRoute, postQueryRoute, postAnnotationRoute)
}
