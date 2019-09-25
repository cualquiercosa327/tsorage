package be.cetic.tsorage.hub.grafanaservice

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.server.directives.DebuggingDirectives
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.io.JsonEOFException
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable.ListBuffer
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object GrafanaBackend {
  val host = "localhost"
  val port = 8080
  val database = FakeDatabase

  /**
   * Parse a JSON string into a Map collection.
   *
   * @param jsonString a JSON string.
   * @return the Map collection corresponding to jsonString.
   */
  def parseJsonString(jsonString: String): Map[String, Any] = {
    parse(jsonString).values.asInstanceOf[Map[String, Any]]
  }

  /**
   * Response to the search request ("/search"). In our case, it is the name of the sensors that is returned.
   *
   * @param request the request in Map format.
   * @return the result of the query request (in this case, the name of sensors).
   */
  private def responseSearchRequest(request: Map[String, Any]): String = {
    database.sensors.mkString("[\"", "\", \"", "\"]")
  }

  /**
   * Handle the search route ("/search").
   * From the official documentation: /search used by the find metric options on the query tab in panels.
   *
   * @param request the request. It is a JSON string.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  private def handleSearchRoute(request: String): StandardRoute = {
    val requestMap = Try(parseJsonString(request))
    requestMap match {
      case Success(_) =>
        // The request was successfully parsed.

        val response = responseSearchRequest(requestMap.get)
        complete(HttpEntity(`application/json`, response))
      case Failure(error) =>
        // Error occurred when running the "parseJsonString" method.
        error match {
          case _: JsonParseException => complete(StatusCodes.BadRequest)
          case _: JsonEOFException => complete(StatusCodes.BadRequest)
        }
    }

    /*
     try {
       val requestMap = parseJsonString(request)
       val response = responseSearchRequest(requestMap)
       complete(HttpEntity(`application/json`, response))
     } catch {
       case _: JsonParseException => complete(StatusCodes.BadRequest)
       case _: JsonEOFException => complete(StatusCodes.BadRequest)
     }
     */

    /*
    // Faster, but longer code. What's the best choice?

    var requestMap: Map[String, Any] = null
    var parsingErrorOccurred = true // We assume that a parsing error will occur.
    try {
      requestMap = parseJsonString(request)
      parsingErrorOccurred = false // No parsing error occurred.
    } catch {
      case _: JsonParseException =>
    }

    if (parsingErrorOccurred) {
      complete(StatusCodes.BadRequest)
    } else {
      val response = responseSearchRequest(requestMap)
      complete(HttpEntity(`application/json`, response))
    }
    */
  }

  /**
   * Response to the query request ("/query").
   * To do this, the database is queried taking into account the parameters.
   *
   * @param request the request in Map format.
   * @return the result of the query request.
   * @throws NoSuchElementException if one or more keys are not found in the request.
   * @throws ClassCastException     if one or more items cannot be casted. For example, request("theValueIsAInt") cannot be
   *                                casted to an integer when it should be.
   */
  private def responseQueryRequest(request: Map[String, Any]): String = {
    var timestampFrom: Long = 0
    var timestampTo: Long = 0
    var interval: Long = 0
    var maxDataPoints = 0
    var sensorNames: List[String] = List()
    try {
      // Retrieve the range of time.
      val timeRange = request("range").asInstanceOf[Map[String, Any]]
      val timeFrom = timeRange("from").toString
      val timeTo = timeRange("to").toString

      // Retrieve date time (ISO 8601) to timestamp in milliseconds.
      timestampFrom = Instant.parse(timeFrom).toEpochMilli
      timestampTo = Instant.parse(timeTo).toEpochMilli

      // Retrieve the interval in milliseconds.
      interval = request("intervalMs").asInstanceOf[Number].longValue()

      // Retrieve the maximum of data points.
      maxDataPoints = request("maxDataPoints").asInstanceOf[Number].intValue()

      // Retrieve the name of the sensors.
      val sensorList = request("targets").asInstanceOf[List[Map[String, String]]]
      sensorNames = for (attribute <- sensorList)
        yield attribute("target")
    } catch {
      case _: NoSuchElementException => throw new NoSuchElementException("Key(s) not found.")
      case _: ClassCastException => throw new ClassCastException("Cannot cast one or more items of the request.")
    }

    // Extract the data from the database in order to response to the request.
    var response: Map[String, List[List[Long]]] = Map()

    for (sensor <- sensorNames) {
      val sensorData = database.extractData(sensor, (timestampFrom / 1000).toInt, (timestampTo / 1000).toInt)

      var sensorDataList = ListBuffer[List[Long]]()
      for ((timestamp, value) <- sensorData) {
        sensorDataList += List(value, timestamp.toLong * 1000)
      }

      response += (sensor -> sensorDataList.toList)
    }

    // Convert (format) the response to a JSON string.
    val formattedResponse = new StringBuilder("[")

    val insideOfResponseList = for ((sensor, sensorDataList) <- response) yield {
      val insideOfResponse = new StringBuilder("{\"target\":\"")
      insideOfResponse.append(sensor)
      insideOfResponse.append("\", \"datapoints\":[")

      /*
      insideOfResponse.append(
        sensorDataList.map(
          singleSensorData =>
            new StringBuilder("[").append(singleSensorData.mkString(",")).toString
        ).mkString("],")
      )
      insideOfResponse.append("]")
      */

      insideOfResponse.append(
        sensorDataList.map(
          _.mkString("[", ",", "]").toString
        ).mkString(",")
        //).mkString("[", ",", "]")
      )

      insideOfResponse.append("]}")
      insideOfResponse
    }
    formattedResponse.append(insideOfResponseList.mkString(", "))
    formattedResponse.append("]")

    formattedResponse.toString
  }

  /**
   * Handle the query route ("/query").
   * From the official documentation: /query should return metrics based on input.
   *
   * @param request the request. It is a JSON string.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  private def handleQueryRoute(request: String): StandardRoute = {
    val requestMap = Try(parseJsonString(request))
    requestMap match {
      case Success(_) =>
        // The request was successfully parsed.

        val response = Try(responseQueryRequest(requestMap.get))
        response match {
          case Success(response) => complete(HttpEntity(`application/json`, response))
          case Failure(error) =>
            error match {
              // Error occurred when casting some items of the request.
              case _: NoSuchElementException => complete(StatusCodes.BadRequest)
              case _: ClassCastException => complete(StatusCodes.BadRequest)
            }
        }
      case Failure(error) =>
        // Error occurred when running the "parseJsonString" method.
        error match {
          case _: JsonParseException => complete(StatusCodes.BadRequest)
          case _: JsonEOFException => complete(StatusCodes.BadRequest)
        }
    }
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher // needed for the future flatMap/ong in the end

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
            entity(as[String]) { request =>
              DebuggingDirectives.logRequestResult("Search route (/search)", Logging.InfoLevel) {
                handleSearchRoute(request)
              }
            }
          }
        },
        // Query route.
        path("query") {
          post {
            entity(as[String]) { request =>
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
