package be.cetic.tsorage.hub.metric


import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.hub.{Cassandra, HubConfig}
import be.cetic.tsorage.hub.filter.{FilterJsonProtocol, Metric, MetricManager}
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.concurrent.ExecutionContext

/**
 * A service for managing metrics.
 */
class MetricHttpService(cassandra: Cassandra)(implicit executionContext: ExecutionContext)
   extends Directives
   with MessageJsonSupport with LazyLogging with FilterJsonProtocol
{
   private val conf = HubConfig.conf

   private val metricManager = MetricManager(cassandra, conf)

   private val session = cassandra.session

   /**
    * @return  The static tagset associated with a metric.
    */
   def getStaticTagset = path("api" / conf.getString("api.version") / "metric" / """([^/]+)""".r / "static-tagset")  {
         metricId =>
         get
         {
            val results = Metric(metricId, session, conf).getStaticTagset()
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
               new Cassandra(conf).updateStaticTagset(metricId, query)
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
                  new Cassandra(conf).setStaticTagset(metricId, query)
                  complete(StatusCodes.NoContent, HttpEntity.Empty)
               }
            }
         }
   }

   /**
    * Looks for specific metrics.
    */
   def postMetricSearch = path("api" / conf.getString("api.version") / "search" / "metric") {
      post {
         entity(as[MetricSearchQuery]) {
            query => {
               val metrics = metricManager.getMetrics(query).map(_.name)
               complete(HttpEntity(ContentTypes.`application/json`, metrics.toJson.compactPrint))
            }
         }
      }
   }

   val routes =
      getStaticTagset ~
      patchStaticTagset ~
      putStaticTagset ~
      postMetricSearch
}
