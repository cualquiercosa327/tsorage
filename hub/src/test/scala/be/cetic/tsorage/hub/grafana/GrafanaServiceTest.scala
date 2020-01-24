package be.cetic.tsorage.hub.grafana

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import be.cetic.tsorage.common.DateTimeConverter
import be.cetic.tsorage.hub.filter.MetricManager
import be.cetic.tsorage.hub.grafana.jsonsupport._
import be.cetic.tsorage.hub.{Cassandra, HubConfig}
import com.typesafe.config.Config
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import spray.json._

class GrafanaServiceTest extends WordSpec with Matchers with BeforeAndAfterAll with ScalatestRouteTest
  with GrafanaJsonSupport {
  // Configuration.
  val conf: Config = HubConfig.conf
    //.withValue("cassandra.keyspaces.raw", ConfigValueFactory.fromAnyRef("tsorage_ts_test")) # Change keyspaces.
    //.withValue("cassandra.keyspaces.other", ConfigValueFactory.fromAnyRef("tsorage_test"))

  // Database handlers.
  val cassandra = new Cassandra(conf)
  val metricManager = MetricManager(cassandra, conf)

  // Grafana service and routes.
  val grafanaService = new GrafanaService(cassandra, metricManager)
  val grafanaConnectionTestRoute: Route = grafanaService.grafanaConnectionTestRoute
  val getMetricNamesRoute: Route = grafanaService.getMetricNamesRoute
  val postMetricNamesRoute: Route = grafanaService.postMetricNamesRoute
  val postQueryRoute: Route = grafanaService.postQueryRoute
  val postAnnotationRoute: Route = grafanaService.postAnnotationRoute

  private val prefix = conf.getString("api.prefix")

  "The Grafana service" should {
    // Grafana connection test route.
    "return OK for GET requests to the Grafana root path" in {
      Get(s"${prefix}/grafana") ~> grafanaConnectionTestRoute ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    // Search route.
    "return the name of all metrics for GET/POST requests to the search path" in {
      Get(s"${prefix}/grafana/search") ~> getMetricNamesRoute ~> check {
        val response = responseAs[SearchResponse]

        response.targets.toSet shouldEqual metricManager.getAllMetrics().map(_.name).toSet
      }

      val request = SearchRequest(Some("Temperature"))
      Post(s"${prefix}/grafana/search", HttpEntity(`application/json`, request.toJson.toString)) ~>
        postMetricNamesRoute ~> check {
        val response = responseAs[SearchResponse]

        response.targets.toSet shouldEqual metricManager.getAllMetrics().map(_.name).toSet
      }
    }

    // Query route (returns success).
    "correctly queries the database for POST requests to the query path" in {
      val request = QueryRequest(
        Seq(Target(Some("temperature")), Target(Some("pressure"))),
        TimeRange("2019-11-30T10:00:00.000Z", "2019-12-02T00:30:00.000Z"),
        None, Some(100)
      )
      Post(s"${prefix}/grafana/query", HttpEntity(`application/json`, request.toJson.toString)) ~> postQueryRoute ~>
        check {
          val response = responseAs[QueryResponse]

          // Test the metric names.
          val metrics = response.dataPointsSeq.map(_.target) // Get the name of metrics.
          metrics.toSet shouldEqual request.targets.flatMap(_.target).toSet

          // Test data.
          val startTimestamp = DateTimeConverter.strToEpochMilli(request.range.from)
          val endTimestamp = DateTimeConverter.strToEpochMilli(request.range.to)
          response.dataPointsSeq.foreach { dataPoints =>
            // Test if there are some data.
            dataPoints.datapoints.size should be > 5

            // Test if data in the response is within the correct time interval.
            dataPoints.datapoints.foreach {
              _._2 should (be >= startTimestamp and be <= endTimestamp)
            }

            // Test if data are ordered according to ascending datetime.
            val timestamps = dataPoints.datapoints.map(_._2)
            timestamps shouldBe sorted
          }

          // Test if there are at most roughly `request.maxDataPoints.get` data.
          response.dataPointsSeq.foreach {
            _.datapoints.size should (be < request.maxDataPoints.get + 5) // Tolerance of five data.
          }
        }
    }

    // Query route (returns an error).
    "correctly handles input errors for POST request to the query path" in {
      // Request with a non-existent-metric.
      val request1 = QueryRequest(
        Seq(Target(Some("non-existent-metric"))),
        TimeRange("2019-11-30T10:00:00.000Z", "2019-12-02T00:30:00.000Z"),
        None, None
      )
      // Request with bad time format ("Z" is missing in this case).
      val request2 = QueryRequest(
        Seq(Target(Some("temperature")), Target(Some("pressure"))),
        TimeRange("2019-11-30T10:00:00.000", "2019-12-02T00:30:00.000Z"),
        None, None
      )
      // Request with an incorrect interval.
      val request3 = QueryRequest(
        Seq(Target(Some("pressure"))),
        TimeRange("2019-11-30T10:00:00.000Z", "2019-10-02T00:30:00.000Z"),
        Some(0), None
      )
      // Request with an incorrect maximum number of data points to keep.
      val request4 = QueryRequest(
        Seq(Target(Some("pressure"))),
        TimeRange("2019-11-30T10:00:00.000Z", "2019-10-02T00:30:00.000Z"),
        None, Some(-1)
      )
      Post(s"${prefix}/grafana/query", HttpEntity(`application/json`, request1.toJson.toString)) ~> postQueryRoute ~>
        check {
          status shouldEqual StatusCodes.NotFound
        }
      Post(s"${prefix}/grafana/query", HttpEntity(`application/json`, request2.toJson.toString)) ~> postQueryRoute ~>
        check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      Post(s"${prefix}/grafana/query", HttpEntity(`application/json`, request3.toJson.toString)) ~> postQueryRoute ~>
        check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      Post(s"${prefix}/grafana/query", HttpEntity(`application/json`, request4.toJson.toString)) ~> postQueryRoute ~>
        check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
    }

    // Annotation route.
    "return an annotation for POST requests to the annotation path" in {
      val annotation = Annotation("my_annotation", enable = true, "SimpleJson", Some("rgba(255, 96, 96, 1)"), None)
      val request = AnnotationRequest(annotation)
      Post(s"${prefix}/grafana/annotations", HttpEntity(`application/json`, request.toJson.toString)) ~>
        postAnnotationRoute ~> check {
        val response = responseAs[AnnotationResponse]

        response.annotations.head.annotation shouldEqual annotation

        val currentTime = System.currentTimeMillis
        response.annotations.head.time shouldEqual currentTime +- 60000 // Tolerance of one minute.
      }
    }
  }
}
