package be.cetic.tsorage.hub.ts

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.hub.Cassandra
import be.cetic.tsorage.hub.filter.{TimeSeriesJsonProtocol, TimeSeriesManager, TimeSeriesQuery, TimeSeriesQueryJsonProtocol}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext

/**
 * A service for managing time series.
 */
case class TimeSeriesService(cassandra: Cassandra)(implicit executionContext: ExecutionContext)
   extends Directives
      with MessageJsonSupport
      with LazyLogging
      with TimeSeriesJsonProtocol
      with TimeSeriesQueryJsonProtocol
{
   private val conf = ConfigFactory.load("hub.conf")

   private val timeseriesManager = TimeSeriesManager(cassandra, conf)

   /**
    * Looks for specific time series.
    */
   def postTimeSeriesSearch = path("api" / conf.getString("api.version") / "search" / "ts") {
      post {
         entity(as[TimeSeriesQuery]) {
            query => {
               println(query)
               val ts = timeseriesManager.getTimeSeries(query)
               complete(HttpEntity(ContentTypes.`application/json`, ts.toJson.compactPrint))
            }
         }
      }
   }

   val routes = postTimeSeriesSearch
}
