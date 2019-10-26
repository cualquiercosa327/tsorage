package be.cetic.tsorage.hub.metric


import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import be.cetic.tsorage.common.Cassandra
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.sharder.Sharder
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import be.cetic.tsorage.hub.filter.{Filter, FilterJsonProtocol, MetricManager, TagFilter}
import com.datastax.driver.core.Session

import scala.concurrent.ExecutionContext
import spray.json._

/**
 * A service for managing metrics.
 */
class MetricHttpService(session: Session)(implicit executionContext: ExecutionContext)
   extends Directives
   with MessageJsonSupport with LazyLogging with FilterJsonProtocol
{
   private val conf = ConfigFactory.load("hub.conf")
   private val sharder = Sharder(conf.getString("sharder"))

   private val metricManager = MetricManager(session, conf)

   /**
    * @return  The static tagset associated with a metric.
    */
   def getStaticTagset = path("api" / conf.getString("api.version") / "metric" / """([^/]+)""".r / "static-tagset")  {
         metricId =>
         get
         {
            val results = metricManager.getStaticTagset(metricId)
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
               Cassandra.updateStaticTagset(metricId, query)
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
                  Cassandra.setStaticTagset(metricId, query)
                  complete(StatusCodes.NoContent, HttpEntity.Empty)
               }
            }
         }
   }

   def postMetricSearch = path("api" / conf.getString("api.version") / "search" / "metrics") {
      post {
         entity(as[MetricSearchQuery]) {
            query => {
               val metrics = query.filter match {
                  case None => metricManager.getAllMetrics()
                  case Some(filter) => metricManager.getMetrics(filter, query.range)

               }

               complete(HttpEntity(ContentTypes.`application/json`, metrics.toJson.compactPrint))
            }
         }
      }
   }


   val routes = getStaticTagset ~
      patchStaticTagset ~
      putStaticTagset ~
      postMetricSearch
}
