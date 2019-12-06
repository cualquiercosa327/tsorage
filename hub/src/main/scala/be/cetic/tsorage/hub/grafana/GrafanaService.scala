package be.cetic.tsorage.hub.grafana

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{Directives, Route}
import be.cetic.tsorage.common.Cassandra
import be.cetic.tsorage.hub.HubConfig
import be.cetic.tsorage.hub.filter.MetricManager
import be.cetic.tsorage.hub.grafana.backend.GrafanaBackend
import be.cetic.tsorage.hub.grafana.jsonsupport.{AnnotationRequest, GrafanaJsonSupport, QueryRequest, SearchRequest}

import scala.concurrent.ExecutionContext

class GrafanaService(cassandra: Cassandra, metricManager: MetricManager)(implicit executionContext: ExecutionContext) extends Directives
  with GrafanaJsonSupport {
  val grafanaRequestHandler = new GrafanaBackend(cassandra, metricManager)

  private val conf = HubConfig.conf
  private val apiVersion = conf.getString("api.version")
  private val apiPrefix = conf.getString("api.prefix")

  /**
   * Grafana connection test route for Grafana. It allows Grafana to test the connection with the server.
   *
   * @return the Grafana connection test route.
   */
  def grafanaConnectionTest: Route = path("api" / apiVersion / "grafana") {
    get {
      DebuggingDirectives.logRequestResult(s"Grafana connection test route (${apiPrefix}/grafana)", Logging.InfoLevel) {
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
  def getMetricNames: Route = path("api" / apiVersion / "grafana" / "search") {
    get {
      DebuggingDirectives.logRequestResult(s"Search route ($apiPrefix/grafana/search)", Logging.InfoLevel) {
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
  def postMetricNames: Route = path("api" / apiVersion / "grafana" / "search") {
    post {
      entity(as[SearchRequest]) { request =>
        DebuggingDirectives.logRequestResult(s"Search route ($apiPrefix/grafana/search)", Logging.InfoLevel) {
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
  def postQuery: Route = path("api" / apiVersion / "grafana" / "query") {
    post {
      entity(as[QueryRequest]) { request =>
        DebuggingDirectives.logRequestResult(s"Query route ($apiPrefix/grafana/query)", Logging.InfoLevel) {
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
  def postAnnotation: Route = path("api" / apiVersion / "grafana" / "annotations") {
    post {
      entity(as[AnnotationRequest]) { request =>
        DebuggingDirectives.logRequestResult(s"Annotation route ($apiPrefix/grafana/annotations)", Logging.InfoLevel) {
          grafanaRequestHandler.handleAnnotationRoute(request)
        }
      }
    }
  }

  val postAnnotationRoute: Route = postAnnotation

  val routes: Route = concat(grafanaConnectionTestRoute, getMetricNamesRoute, postMetricNamesRoute, postQueryRoute,
    postAnnotationRoute)
}
