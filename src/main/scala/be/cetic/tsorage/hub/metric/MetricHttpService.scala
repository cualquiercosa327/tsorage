package be.cetic.tsorage.hub.metric

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import be.cetic.tsorage.hub.auth.{AuthenticationQuery, MessageJsonSupport}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import be.cetic.tsorage.hub.Cassandra
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
   /**
    * @return  The static tagset associated with a metric.
    */
   def getStaticTagset = path("metric" / """(\w+)""".r / "tagset")  {
         metricId =>
         get
         {
            val results = Cassandra.getStaticTagset(metricId)

            results match {
               case Some(r) => complete(HttpEntity(ContentTypes.`application/json`, r.toJson.compactPrint))
               case None => complete(StatusCodes.NotFound)
            }
         }
      }

   /**
    * Changes some static tags of a particular metric.
    */
   def patchStaticTagset = path("metric" / """(\w+)""".r / "tagset") {
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
   def putStaticTagset = path("metric" / """(\w+)""".r / "tagset") {
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
   def getMetrics = path("metric") {
      get
      {
         val result = Cassandra.getAllMetrics()
         complete(HttpEntity(ContentTypes.`application/json`, result.toJson.compactPrint))
      }
   }

   val routes = getStaticTagset ~ patchStaticTagset ~ putStaticTagset ~ getMetrics
}
