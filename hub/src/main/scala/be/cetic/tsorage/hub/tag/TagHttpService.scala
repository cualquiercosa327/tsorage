package be.cetic.tsorage.hub.tag

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.DebuggingDirectives
import be.cetic.tsorage.common.Cassandra
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.hub.HubConfig
import be.cetic.tsorage.hub.filter.{FilterJsonProtocol, MetricManager, TagManager}
import be.cetic.tsorage.hub.metric.MetricSearchQuery
import be.cetic.tsorage.hub.tag.TagValueQuery
import com.datastax.driver.core.Session
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import spray.json._

/**
 * A service for managing tags.
 */
class TagHttpService(val cassandra: Cassandra)(implicit executionContext: ExecutionContext)
   extends Directives
      with MessageJsonSupport
      with LazyLogging
      with FilterJsonProtocol
{
   private val conf = HubConfig.conf
   private val tagManager = TagManager(cassandra, conf)

   private val session = cassandra.session


   /**
    * Provide the list of available values for a static tag name, with the names of the associated metric.
    */
   def getStatictagValues = path("api" / conf.getString("api.version") / "static-tag" / """(\w+)""".r / "values") {
      tagname =>
         get {
            DebuggingDirectives.logRequest(s"Values of static tag name ${tagname} are queried ", Logging.InfoLevel) {
               val results = new Cassandra(conf).getStaticTagValues(tagname)

               if(results.isEmpty) complete(StatusCodes.NoContent, HttpEntity.Empty)
               else complete(HttpEntity(ContentTypes.`application/json`, results.toJson.compactPrint))
            }
         }
   }

   def postTagnameSuggestion = path("api" / conf.getString("api.version") / "search" / "tagnames") {
      post {
         entity(as[MetricSearchQuery]) {
            query => {
               val tagNames: Set[String] = query.filter match {
                  case None => tagManager.getAllTagNames(query.range)
                  case Some(filter) => tagManager.suggestTagNames(filter, query.range)
               }

               complete(HttpEntity(ContentTypes.`application/json`, tagNames.toJson.compactPrint))
            }
         }
      }
   }

   def postTagvalueSuggestion = path("api" / conf.getString("api.version") / "search" / "tagvalues") {
      post {
         entity(as[TagValueQuery]) {
            query => {
               val tagValues = tagManager.suggestTagValues(query)

               complete(HttpEntity(ContentTypes.`application/json`, tagValues.toJson.compactPrint))
            }
         }
      }
   }


   val routes =
      getStatictagValues ~
      postTagnameSuggestion ~
      postTagvalueSuggestion
}
