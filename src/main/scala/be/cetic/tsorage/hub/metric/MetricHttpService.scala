package be.cetic.tsorage.hub.metric

import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import be.cetic.tsorage.hub.auth.{AuthenticationQuery, MessageJsonSupport}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.DebuggingDirectives
import be.cetic.tsorage.hub.Cassandra
import be.cetic.tsorage.hub.grafana.jsonsupport.AnnotationRequest
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

/**
 * A service for managing metrics.
 */
class MetricHttpService(implicit executionContext: ExecutionContext)
   extends Directives
      with MessageJsonSupport with LazyLogging
{
   private val conf = ConfigFactory.load("hub.conf")

   /**
    * @return  The static tagset associated with a metric.
    */
   def getStaticTagset = path("api" / conf.getString("api.version") / "metric" / """(\w+)""".r / "tagset")  {
         metricId =>
         get
         {
            val results = Cassandra.getStaticTagset(metricId)
            complete(HttpEntity(ContentTypes.`application/json`, results.toJson.compactPrint))
         }
      }

   /**
    * Changes some static tags of a particular metric.
    */
   def patchStaticTagset = path("api" / conf.getString("api.version") / "metric" / """(\w+)""".r / "tagset") {
      metricId =>
      patch
      {
         entity(as[Map[String, String]])
         {
            query => {
               logger.info(s"Update static tagset for ${metricId}: ${query}")
               Cassandra.updateStaticTagset(metricId, query)
               complete(StatusCodes.NoContent, HttpEntity.Empty)
            }
         }
      }
   }

   /**
    * Exhaustively sets the static tagset of a metric. Any tag that is not in the submitted list is discarded.
    */
   def putStaticTagset = path("api" / conf.getString("api.version") /"metric" / """(\w+)""".r / "tagset") {
      metricId =>
         put
         {
            entity(as[Map[String, String]])
            {
               query => {
                  logger.info(s"Set static tagset for ${metricId}: ${query}")
                  Cassandra.setStaticTagset(metricId, query)
                  complete(StatusCodes.NoContent, HttpEntity.Empty)
               }
            }
         }
   }

   /**
    * Provide a list of all reachable metric.
    */
   def getMetricSearch = path("api" / conf.getString("api.version") / "metric" / "search") {
      get {

         parameterMap{
            params => {
               DebuggingDirectives.logRequest(s"Metric Search with static taget ${params}", Logging.InfoLevel) {
                  println(params)
                  val result = Cassandra.getMetricsWithStaticTagset(params)
                  complete(HttpEntity(ContentTypes.`application/json`, result.toList.toJson.compactPrint))
               }
            }
         }
      }
   }

   val routes = getStaticTagset ~ patchStaticTagset ~ putStaticTagset ~ getMetricSearch
}
