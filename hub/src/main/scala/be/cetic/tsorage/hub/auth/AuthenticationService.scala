package be.cetic.tsorage.hub.auth

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directives, RouteConcatenation}
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.AuthenticationQuery
import be.cetic.tsorage.hub.HubConfig
import be.cetic.tsorage.hub.auth.backend.AuthenticationBackend
import spray.json._

import scala.concurrent.ExecutionContext
/**
 * A service for managing the authentication.
 *
 * It receives a token, and retrieves the user account associated with this token, if any.
 *
 *
 * Ex:
 *
 * POST http://localhost:8081/auth
 * Content-Encoding: deflate
 * Content-Type: application/json
 *
 * {"api_key": "4b8639ed-0e90-4b3f-8a45-e87c22d17887"}
 *
 * == ANSWER
 *
 * HTTP/1.1 200 OK
 * Date: Tue, 31 Oct 2006 08:00:29 GMT
 * Connection: close
 * Allow: POST
 * Content-Length: 0
 * Content-Type: application/json
 *
 * {"user": {"id": 421, "name": "Mathieu Goeminne"} }
 */
class AuthenticationService(implicit executionContext: ExecutionContext) extends Directives with MessageJsonSupport with RouteConcatenation
{
   val conf = HubConfig.conf
   val authenticator = AuthenticationBackend(conf.getConfig("backend"))

   def postAuth = path("api" / conf.getString("api.version") / "auth") {
      post
      {
         entity(as[AuthenticationQuery])
         {
            query => complete(HttpEntity(ContentTypes.`application/json`, authenticator.check(query.api_key).toJson.compactPrint))
         }
      }
   }

   val route = postAuth
}
