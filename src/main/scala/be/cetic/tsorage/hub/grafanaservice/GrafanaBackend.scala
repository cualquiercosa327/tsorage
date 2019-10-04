package be.cetic.tsorage.hub.grafanaservice

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.{Directives, StandardRoute}
import akka.http.scaladsl.server.directives.DebuggingDirectives

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object GrafanaBackend extends Directives with JsonSupport {
  val host = "localhost"
  val port = 8080
  val database: FakeDatabase.type = FakeDatabase

  /**
   * Response to the search request ("/search"). In our case, it is the name of the sensors that is returned.
   *
   * @param request the search request.
   * @return the response to the search request (in this case, the name of sensors).
   */
  private def responseSearchRequest(request: SearchRequest): SearchResponse = {
    SearchResponse(database.sensors.toList)
  }

  /**
   * Handle the search route ("/search").
   *
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
   * Aggregate data points by keeping only the first one (the remaining data is dropped).
   *
   * Each data is composed of a timestamp and a value. Therefore, the ith value of `values` correspond to the ith
   * timestamp of `timestamps`.
   *
   * @param timestamps a sequence of timestamps (in milliseconds).
   * @param values     a sequence of values.
   * @return the aggregation of timestamps and the aggregation of values if `timestamps` and `values` are nonempty.
   *         Otherwise, return None.
   * @throws IllegalArgumentException if `timestamps` and `values` does not have the same length.
   */
  def aggregateDataPointsByDropping(timestamps: Seq[Long], values: Seq[BigDecimal]): Option[(Long, BigDecimal)] = {
    if (timestamps.size != values.size) {
      throw new IllegalArgumentException(s"Invalid sequence length, $timestamps and $values must be have the same " +
        s"length.")
    }

    if (timestamps.size < 1) { // `timestamps.size` and `values.size` are equal here.
      return None
    }

    Some(timestamps.head, values.head)
  }

  /**
   * Aggregate data points by averaging them.
   *
   * Each data is composed of a timestamp and a value. Therefore, the ith value of `values` correspond to the ith
   * timestamp of `timestamps`.
   *
   * @param timestamps a sequence of timestamps (in milliseconds).
   * @param values     a sequence of values.
   * @return the aggregation of timestamps and the aggregation of values if `timestamps` and `values` are nonempty.
   *         Otherwise, return None.
   * @throws IllegalArgumentException if `timestamps` and `values` does not have the same length.
   */
  def aggregateDataPointsByAveraging(timestamps: Seq[Long], values: Seq[BigDecimal]): Option[(Long, BigDecimal)] = {
    if (timestamps.size != values.size) {
      throw new IllegalArgumentException(s"Invalid sequence length, $timestamps and $values must be have the same " +
        s"length.")
    }

    if (timestamps.size < 1) { // `timestamps.size` and `values.size` are equal here.
      return None
    }

    Some(
      (timestamps.sum / timestamps.size.toDouble).toLong,
      (values.sum / values.size.toDouble).toLong
    )
  }

  /**
   * Handle the "max data points" feature for Grafana (reducing of the number of data points to `maxNumDataPoints`).
   *
   * In our case, this function aggregates data points to ensure that there are at most `maxNumDataPoints` points. To
   * do this, a aggregation function is used (`aggregationFunc`).
   *
   * Supposition: `dataPoints` is sorted by timestamp in ascending order.
   *
   * Example: suppose there are 3000 data points and `maxNumDataPoints` is equal to 1000. Therefore, the 3000 data
   * points will be aggregated into 1000 data points (in this example, every three consecutive data points will be
   * aggregated).
   *
   * @param dataPoints       the data points sorted by timestamps in ascending order.
   * @param maxNumDataPoints the maximum number of data points to keep.
   * @param aggregationFunc  an aggregation function aggregating multiple timestamps/values into a single one. This
   *                         function takes two parameters: the first one is a sequence of timestamps (in
   *                         milliseconds) and the second one is a sequence of values. It returns a tuple containing
   *                         the aggregation of timestamps and the aggregation of values if the sequence of
   *                         timestamps and the sequence of values are nonempty. It returns None otherwise.
   * @return a DataPoints object containing a maximum of `maxNumDataPoints` data points.
   */
  def handleMaxDataPoints(dataPoints: DataPoints, maxNumDataPoints: Int,
                          aggregationFunc: (Seq[Long], Seq[BigDecimal]) => Option[(Long, BigDecimal)]): DataPoints = {

    val numDataPoints = dataPoints.datapoints.size
    if (numDataPoints <= maxNumDataPoints) {
      // It is not necessary to aggregate data points.
      return dataPoints
    }

    // Here: numDataPoints > maxNumDataPoints.

    val dataPointRatio = numDataPoints / maxNumDataPoints.toDouble

    // Aggregate every `dataPointsRatio` consecutive data points (approximately).
    var dataPointTempList: List[(BigDecimal, Long)] = List()
    val dataPointList = dataPoints.datapoints.zipWithIndex.flatMap {
      case (singleData, i) =>
        // Add the data to the temporary list.
        dataPointTempList = singleData +: dataPointTempList

        if (i % dataPointRatio < 1 || i == numDataPoints - 1) {
          // Get value and timestamp of each data in temporary list.
          val timestamps = dataPointTempList.map(_._2)
          val values = dataPointTempList.map(_._1)

          // Aggregate the data contained in the temporary list.
          val aggregatedData = aggregationFunc(timestamps, values)

          // Empty the temporary list.
          dataPointTempList = List()

          aggregatedData match {
            case Some((aggregatedTimestamp, aggregatedValue)) =>
              // Convert the aggregated data for Grafana.
              Some(Tuple2[BigDecimal, Long](aggregatedValue, aggregatedTimestamp))
            case _ => None
          }
        } else {
          None
        }
    }

    DataPoints(dataPoints.target, dataPointList)
  }

  /**
   * Handle the "interval" feature for Grafana (data points are `interval` milliseconds apart).
   *
   * In our case, this function aggregates all data points within an `interval` milliseconds interval. To do this,
   * a aggregation function is used (`aggregationFunc`).
   *
   * Supposition: `dataPoints` is sorted by timestamp in ascending order.
   *
   * @param dataPoints      the data points sorted by timestamps in ascending order.
   * @param interval        the interval in milliseconds.
   * @param aggregationFunc an aggregation function aggregating multiple timestamps/values into a single one. This
   *                        function takes two parameters: the first one is a sequence of timestamps (in
   *                        milliseconds) and the second one is a sequence of values. It returns a tuple containing
   *                        the aggregation of timestamps and the aggregation of values if the sequence of
   *                        timestamps and the sequence of values are nonempty. It returns None otherwise.
   * @return a DataPoints object containing a maximum of `maxNumDataPoints` data points.
   */
  def handleInterval(dataPoints: DataPoints, interval: Long,
                     aggregationFunc: (Seq[Long], Seq[BigDecimal]) => Option[(Long, BigDecimal)]): DataPoints = {

    val intervalSizeMax = interval // Just use another name for this in order to better understand the code.

    val numDataPoints = dataPoints.datapoints.size

    // Aggregate all data points within an `intervalMax` milliseconds interval.
    var previousTimestamp: Long = 0
    var intervalSize: Long = 0
    var dataPointTempList: List[(BigDecimal, Long)] = List()
    val dataPointList = dataPoints.datapoints.zipWithIndex.flatMap {
      case (singleData, i) =>
        // Add the data to the temporary list.
        dataPointTempList = singleData +: dataPointTempList

        // Extract the timestamp of this data.
        val currentTimestamp = singleData._2

        // Compute the difference between the current timestamp and the previous one and add it to `intervalSize`.
        val timestampDiff = currentTimestamp - previousTimestamp
        intervalSize += timestampDiff

        // Update the previous timestamp.
        previousTimestamp = currentTimestamp

        if (intervalSize > intervalSizeMax || i == numDataPoints - 1) {
          // Get value and timestamp of each data in temporary list.
          val timestamps = dataPointTempList.map(_._2)
          val values = dataPointTempList.map(_._1)

          // Aggregate the data contained in the temporary list.
          val aggregatedData = aggregationFunc(timestamps, values)

          // Reset some variables.
          intervalSize = 0
          dataPointTempList = List()

          aggregatedData match {
            case Some((aggregatedTimestamp, aggregatedValue)) =>
              // Convert the aggregated data for Grafana.
              Some(Tuple2[BigDecimal, Long](aggregatedValue, aggregatedTimestamp))
            case _ => None
          }
        } else {
          None
        }
    }

    DataPoints(dataPoints.target, dataPointList)
  }

  /**
   * Response to the query request ("/query").
   *
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
    val sensors = request.targets.flatMap(_.target)

    // Extract the data from the database in order to response to the request.
    var dataPointsList = List[DataPoints]()
    for (sensor <- sensors) {
      // Extract data for this sensor.
      val sensorData = database.extractData(sensor, (timestampFrom / 1000).toInt, (timestampTo / 1000).toInt)

      // Retrieve the data points from the sensor data.
      val dataPoints = for ((timestamp, value) <- sensorData)
        yield Tuple2[BigDecimal, Long](value, timestamp.toLong * 1000)

      // Prepend all data points for this sensor to the list of data points.
      dataPointsList = DataPoints(sensor, dataPoints) +: dataPointsList
    }

    // Handle the "interval" feature for Grafana (if the request contains a field "intervalMs").
    request.intervalMs match {
      case Some(interval) =>
        dataPointsList = dataPointsList.map(dataPoints =>
          handleInterval(dataPoints, interval, aggregateDataPointsByAveraging)
        )
      case _ =>
    }

    // Handle the "max data points" feature for Grafana (if the request contains a field "maxDataPoints").
    request.maxDataPoints match {
      case Some(maxDataPoints) =>
        dataPointsList = dataPointsList.map(dataPoints =>
          handleMaxDataPoints(dataPoints, maxDataPoints, aggregateDataPointsByAveraging)
        )
      case _ =>
    }

    QueryResponse(dataPointsList)
  }

  /**
   * Handle the query route ("/query").
   *
   * From the official documentation: /query should return metrics based on input.
   *
   * @param request the query request.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  private def handleQueryRoute(request: QueryRequest): StandardRoute = {
    val response = responseQueryRequest(request)
    complete(response)
  }

  /**
   * Response to the annotation request ("/annotations").
   *
   * @param request the annotation request.
   * @return the response to the annotation request.
   */
  private def responseAnnotationRequest(request: AnnotationRequest): AnnotationResponse = {
    AnnotationResponse(
      List(
        AnnotationObject(request.annotation, "Marker", System.currentTimeMillis)
      )
    )
  }

  /**
   * Handle the annotation route ("/annotations").
   *
   * From the official documentation: /annotations should return annotations.
   *
   * @param request the annotation request.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  private def handleAnnotationRoute(request: AnnotationRequest): StandardRoute = {
    val response = responseAnnotationRequest(request)
    complete(response)
  }

  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

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
        },
        // Annotation route.
        path("annotations") {
          post {
            entity(as[AnnotationRequest]) { request =>
              DebuggingDirectives.logRequestResult("Annotation route (/annotations)", Logging.InfoLevel) {
                handleAnnotationRoute(request)
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
