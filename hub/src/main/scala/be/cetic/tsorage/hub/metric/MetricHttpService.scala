package be.cetic.tsorage.hub.metric

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.DebuggingDirectives
import be.cetic.tsorage.common.Cassandra
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.sharder.Sharder
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import spray.json._

/**
 * A service for managing metrics.
 */
class MetricHttpService(implicit executionContext: ExecutionContext)
   extends Directives
   with MessageJsonSupport with LazyLogging
{
   private val conf = ConfigFactory.load("hub.conf")
   private val sharder = Sharder(conf.getString("sharder"))

   /**
    * @return  The static tagset associated with a metric.
    */
   def getStaticTagset = path("api" / conf.getString("api.version") / "metric" / """([^/]+)""".r / "static-tagset")  {
         metricId =>
         get
         {
            val results = new Cassandra().getStaticTagset(metricId)
            complete(HttpEntity(ContentTypes.`application/json`, results.toJson.compactPrint))
         }
      }

   /**
    * Changes some static tags of a particular metric.
    */
   def patchStaticTagset = path("api" / conf.getString("api.version") / "metric" / """([^/]+)""".r / "static-tagset") {
      metricId =>
      patch
      {
         entity(as[Map[String, String]])
         {
            query => {
               logger.info(s"Update static tagset for ${metricId}: ${query}")
               new Cassandra().updateStaticTagset(metricId, query)
               complete(StatusCodes.NoContent, HttpEntity.Empty)
            }
         }
      }
   }

   /**
    * Exhaustively sets the static tagset of a metric. Any tag that is not in the submitted list is discarded.
    */
   def putStaticTagset = path("api" / conf.getString("api.version") /"metric" / """([^/]+)""".r / "static-tagset") {
      metricId =>
         put
         {
            entity(as[Map[String, String]])
            {
               query => {
                  logger.info(s"Set static tagset for ${metricId}: ${query}")
                  new Cassandra().setStaticTagset(metricId, query)
                  complete(StatusCodes.NoContent, HttpEntity.Empty)
               }
            }
         }
   }

   /**
    * Provide a list of all reachable metrics.
    */
   def getMetricSearch = path("api" / conf.getString("api.version") / "metric" / "search") {
      get {

         parameterMap{
            params => {
               DebuggingDirectives.logRequest(s"Metric Search with static taget ${params}", Logging.InfoLevel) {
                  val result = new Cassandra().getMetricsWithStaticTagset(params)
                  complete(HttpEntity(ContentTypes.`application/json`, result.toList.toJson.compactPrint))
               }
            }
         }
      }
   }

   /**
    * Provide a list of dynamic tags covering the specified time period.
    * This list is approximate, since dynamic tagsets are recorded by shard.
    * In other words, the list may content tags that are not really associated with the considered period,
    * but are associated with (at least) one the shards covering that period.
    * @return A superset of the tags dynamically associated with the given metric and time period.
    */
   def getDynamicTags = path("api" / conf.getString("api.version") / "metric" / """([^/]+)""".r / "dynamic-tagset"){
      metricId =>
         get
         {
            parameterMap{
               params => {
                  DebuggingDirectives.logRequest(s"Asks for the dynamic tagset of ${metricId} with ${parameterMap}", Logging.InfoLevel) {

                     if( !(params contains "from") || !(params contains "to")) complete(StatusCodes.BadRequest, HttpEntity.Empty)

                     val start = LocalDateTime.parse(params("from"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                     val end = LocalDateTime.parse(params("to"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                     val shards = sharder.shards(start, end).toSet

                     val result = new Cassandra().getDynamicTagset(metricId, shards)

                     complete(HttpEntity(ContentTypes.`application/json`, result.toJson.compactPrint))
                  }
               }
            }
         }
   }

   /**
    * Provide a list of all metrics having all of the specified dynamic tags
    * in a specified time range.
    *
    * If no dynamic tags are specified, then no metric is retrieved.
    */
   def getMetricDynamicSearch = path("api" / conf.getString("api.version") / "metric" / "dynamic-search") {
      get {
         parameterMap{
            params => {
               DebuggingDirectives.logRequest(s"Metric Search with dynamic tagset ${params}", Logging.InfoLevel) {
                  if(
                     !(params contains "from") ||
                     !(params contains "to")
                  ) complete(StatusCodes.BadRequest, HttpEntity.Empty)

                  val tags = params.keySet
                     .filterNot(k => k == "from" || k == "to")
                     .map(k => k -> params(k))

                  val from = LocalDateTime.parse(params("from"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                  val to = LocalDateTime.parse(params("to"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                  val result = if(tags.isEmpty) Set.empty[String]
                               else tags.par
                                 .map(tag => new Cassandra().getMetricsWithDynamigTag(tag._1, tag._2, from, to))
                                 .reduce(_ intersect _)


                  complete(HttpEntity(ContentTypes.`application/json`, result.toJson.compactPrint))
               }
            }
         }
      }
   }

   val routes = getStaticTagset ~
      patchStaticTagset ~
      putStaticTagset ~
      getMetricSearch ~
      getDynamicTags ~
      getMetricDynamicSearch
}
