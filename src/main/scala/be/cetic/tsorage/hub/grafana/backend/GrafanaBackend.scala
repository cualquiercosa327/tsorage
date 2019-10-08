package be.cetic.tsorage.hub.grafana.backend

import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, StandardRoute}
import be.cetic.tsorage.hub.grafana.FakeDatabase
import be.cetic.tsorage.hub.grafana.jsonsupport.{
  AnnotationObject, AnnotationRequest, AnnotationResponse, DataPoints,
  GrafanaJsonSupport, QueryRequest, QueryResponse, SearchRequest, SearchResponse
}

import scala.util.{Failure, Success, Try}

object GrafanaBackend extends Directives with GrafanaJsonSupport {
  val database: FakeDatabase.type = FakeDatabase

  /**
   * Response to the search request ("/search"). In our case, it is the name of the metrics that is returned.
   *
   * @param request the search request.
   * @return the response to the search request (in this case, the name of metrics).
   */
  def responseSearchRequest(request: Option[SearchRequest]): Try[SearchResponse] = {
    Success(SearchResponse(database.metrics.toList))
  }

  /**
   * Handle the search route ("/search").
   *
   * From the Grafana's official documentation: /search used by the find metric options on the query tab in panels.
   *
   * @param request the search request.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  def handleSearchRoute(request: Option[SearchRequest]): StandardRoute = {
    val response = responseSearchRequest(request)
    response match {
      case Success(resp) => complete(resp)
      case _ => complete(StatusCodes.InternalServerError -> "Unexpected error.")
    }
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
   *         Otherwise, return None. If `timestamps` and `values` does not have the same length, then
   *         *         `Failure(java.lang.IllegalArgumentException)` is returned.
   * @throws IllegalArgumentException if `timestamps` and `values` does not have the same length.
   */
  def aggregateDataPointsByDropping(timestamps: Seq[Long], values: Seq[BigDecimal]): Try[Option[(Long, BigDecimal)]] = {
    if (timestamps.size != values.size) {
      return Failure(new IllegalArgumentException(s"Invalid sequence length, $timestamps and $values must be have the" +
        s" same length."))
    }

    if (timestamps.size < 1) { // `timestamps.size` and `values.size` are equal here.
      return Success(None)
    }

    Success(Some(timestamps.head, values.head))
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
   *         Otherwise, return None. If `timestamps` and `values` does not have the same length, then
   *         `Failure(java.lang.IllegalArgumentException)` is returned.
   */
  def aggregateDataPointsByAveraging(timestamps: Seq[Long],
                                     values: Seq[BigDecimal]): Try[Option[(Long, BigDecimal)]] = {
    if (timestamps.size != values.size) {
      return Failure(new IllegalArgumentException(s"Invalid sequence length, $timestamps and $values must be have the" +
        s" same length."))
    }

    if (timestamps.size < 1) { // `timestamps.size` and `values.size` are equal here.
      return Success(None)
    }

    Success(
      Some(
        (timestamps.sum / timestamps.size.toDouble).toLong,
        (values.sum / values.size.toDouble).toLong
      )
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
   *                         timestamps and the sequence of values are nonempty. It returns None otherwise. If an
   *                         error occurs, then `Failure(...)` is returned.
   * @return a DataPoints object containing a maximum of `maxNumDataPoints` data points.
   */
  def handleMaxDataPoints(dataPoints: DataPoints, maxNumDataPoints: Int,
                          aggregationFunc: (Seq[Long], Seq[BigDecimal]) =>
                            Try[Option[(Long, BigDecimal)]]): DataPoints = {

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
            case Success(data) =>
              data match {
                case Some((aggregatedTimestamp, aggregatedValue)) =>
                  // Convert the aggregated data for Grafana.
                  Some(Tuple2[BigDecimal, Long](aggregatedValue, aggregatedTimestamp))
                case _ => None
              }
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
   *                        timestamps and the sequence of values are nonempty. It returns None otherwise. If an
   *                        error occurs, then `Failure(...)` is returned.
   * @return a DataPoints object containing a maximum of `maxNumDataPoints` data points.
   */
  def handleInterval(dataPoints: DataPoints, interval: Long,
                     aggregationFunc: (Seq[Long], Seq[BigDecimal]) => Try[Option[(Long, BigDecimal)]]): DataPoints = {

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
            case Success(data) =>
              data match {
                case Some((aggregatedTimestamp, aggregatedValue)) =>
                  // Convert the aggregated data for Grafana.
                  Some(Tuple2[BigDecimal, Long](aggregatedValue, aggregatedTimestamp))
                case _ => None
              }
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
   * @return the response to the query request. If the request contains at least one metric that does not appear in
   *         the database, then `Failure(java.lang.IllegalArgumentException)` is returned.
   */
  def responseQueryRequest(request: QueryRequest): Try[QueryResponse] = {
    // Convert date time (ISO 8601) to timestamp in milliseconds.
    val timestampFrom = Instant.parse(request.range.from).toEpochMilli
    val timestampTo = Instant.parse(request.range.to).toEpochMilli

    // Get the name of metrics.
    val metrics = request.targets.flatMap(_.target)

    if (!metrics.toSet.subsetOf(database.metrics.toSet)) {
      return Failure(new IllegalArgumentException(s"${request.targets} contains at least one metric that does not " +
        s"appear in the database."))
    }

    // Extract the data from the database in order to response to the request.
    var dataPointsList = List[DataPoints]()
    for (metric <- metrics) {
      // Extract data for this metric.
      val metricData = database.extractData(metric, (timestampFrom / 1000).toInt, (timestampTo / 1000).toInt)

      // Retrieve the data points from the metric data.
      val dataPoints = for ((timestamp, value) <- metricData)
        yield Tuple2[BigDecimal, Long](value, timestamp.toLong * 1000)

      // Prepend all data points for this metric to the list of data points.
      dataPointsList = DataPoints(metric, dataPoints) +: dataPointsList
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

    Success(QueryResponse(dataPointsList))
  }

  /**
   * Handle the query route ("/query").
   *
   * From the Grafana's official documentation: /query should return metrics based on input.
   *
   * @param request the query request.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  def handleQueryRoute(request: QueryRequest): StandardRoute = {
    val response = responseQueryRequest(request)
    response match {
      case Success(resp) => complete(resp)
      case Failure(_: IllegalArgumentException) => complete(StatusCodes.MethodNotAllowed -> "Invalid input.")
      case _ => complete(StatusCodes.InternalServerError -> "Unexpected error.")
    }
  }

  /**
   * Response to the annotation request ("/annotations").
   *
   * @param request the annotation request.
   * @return the response to the annotation request.
   */
  def responseAnnotationRequest(request: AnnotationRequest): Try[AnnotationResponse] = {
    Success(
      AnnotationResponse(
        List(AnnotationObject(request.annotation, "Marker", System.currentTimeMillis))
      )
    )
  }

  /**
   * Handle the annotation route ("/annotations").
   *
   * From the Grafana's official documentation: /annotations should return annotations.
   *
   * @param request the annotation request.
   * @return a Standard route (for Akka HTTP). It is the response to the request.
   */
  def handleAnnotationRoute(request: AnnotationRequest): StandardRoute = {
    val response = responseAnnotationRequest(request)
    response match {
      case Success(resp) => complete(resp)
      case _ => complete(StatusCodes.InternalServerError -> "Unexpected error.")
    }
  }
}
