package be.cetic.tsorage.hub.grafanaservice

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.{Directives, StandardRoute}
import akka.http.scaladsl.server.directives.DebuggingDirectives
import scala.io.StdIn

object GrafanaBackend extends Directives with JsonSupport {
  val host = "localhost"
  val port = 8080
  val database = FakeDatabase

  /**
   * Response to the search request ("/search"). In our case, it is the name of the sensors that is returned.
   *
   * @param request the search request.
   * @return the response to the search request (in this case, the name of sensors).
   */
  private def responseSearchRequest(request: SearchRequest): SearchResponse = {
    //database.sensors.mkString("[\"", "\", \"", "\"]")
    SearchResponse(database.sensors.toList)
  }

  /**
   * Handle the search route ("/search").
   * From the official documentation: /search used by the find metric options on the query tab in panels.
   *
   * @param request the search request.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  private def handleSearchRoute(request: SearchRequest): StandardRoute = {
    val response = responseSearchRequest(request)
    complete(response)
  }

  /**
   * Response to the query request ("/query").
   * To do this, the database is queried taking into account the parameters.
   *
   * @param request the query request.
   * @return the response to the query request.
   */
  private def responseQueryRequest(request: QueryRequest): QueryResponse = {
    // Convert date time (ISO 8601) to timestamp in milliseconds.
    val timestampFrom = Instant.parse(request.range.from).toEpochMilli
    val timestampTo = Instant.parse(request.range.to).toEpochMilli

    // Get the name of sensors.
    var sensors = List[String]()
    for (target <- request.targets) {
      sensors = target.target match {
        case Some(name) =>
          name +: sensors
        case _ => sensors
      }
    }

    // Extract the data from the database in order to response to the request.
    var dataPointsList = List[DataPoints]()
    for (sensor <- sensors) {
      // Extract data for this sensor.
      val sensorData = database.extractData(sensor, (timestampFrom / 1000).toInt, (timestampTo / 1000).toInt)
      println(sensorData)

      // Retrieve the data points from the sensor data.
      val dataPoints = for ((timestamp, value) <- sensorData)
        yield List[BigDecimal](value, timestamp.toLong * 1000)

      // Prepend all data points for this sensor to the list of data points.
      dataPointsList = DataPoints(sensor, dataPoints.toList) +: dataPointsList
    }

    QueryResponse(dataPointsList)
  }

  /**
   * Handle the query route ("/query").
   * From the official documentation: /query should return metrics based on input.
   *
   * @param request the query request.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  private def handleQueryRoute(request: QueryRequest): StandardRoute = {
    val response = responseQueryRequest(request)
    complete(response)
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    // Create all routes.
    val routes =
      concat(
        // Route to test the connection.
        path("") {
          get {
            DebuggingDirectives.logRequestResult("Connection test route (/)", Logging.InfoLevel) {
              complete(StatusCodes.OK)
            }
          }
        },
        // Search route.
        path("search") {
          post {
            entity(as[SearchRequest]) { request =>
              DebuggingDirectives.logRequestResult("Search route (/search)", Logging.InfoLevel) {
                handleSearchRoute(request)
              }
            }
          }
        },
        // Query route.
        path("query") {
          post {
            entity(as[QueryRequest]) { request =>
              DebuggingDirectives.logRequestResult("Query route (/query)", Logging.InfoLevel) {
                handleQueryRoute(request)
              }
            }
          }
        }
      )

    // Bind and handle all routes to the host and the port.
    val bindingFuture = Http().bindAndHandle(routes, host, port)

    println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
