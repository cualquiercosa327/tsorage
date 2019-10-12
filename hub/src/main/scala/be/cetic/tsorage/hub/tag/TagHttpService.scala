package be.cetic.tsorage.hub.tag

import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.DebuggingDirectives
import be.cetic.tsorage.hub.Cassandra
import be.cetic.tsorage.hub.auth.MessageJsonSupport
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

import spray.json._

/**
 * A service for managing tags.
 */
class TagHttpService(implicit executionContext: ExecutionContext) extends Directives with MessageJsonSupport with LazyLogging
{
   private val conf = ConfigFactory.load("hub.conf")

   /**
    * Provide the list of available values for a static tag name, with the names of the associated metric.
    */
   def getStatictagValues = path("api" / conf.getString("api.version") / "statictag" / """(\w+)""".r / "values") {
      tagname =>
         get {
            DebuggingDirectives.logRequest(s"Values of static tag name ${tagname} are queried ", Logging.InfoLevel) {
               val results = Cassandra.getStaticTagValues(tagname)

               if(results.isEmpty) complete(StatusCodes.NoContent, HttpEntity.Empty)
               else complete(HttpEntity(ContentTypes.`application/json`, results.toJson.compactPrint))
            }
         }
   }

   val routes = getStatictagValues
}
