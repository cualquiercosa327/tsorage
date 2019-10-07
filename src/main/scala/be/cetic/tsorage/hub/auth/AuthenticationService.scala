package be.cetic.tsorage.hub.auth

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.directives.HeaderDirectives
import akka.http.scaladsl.server.{Directive, Directives}
import akka.pattern.ask
import akka.util.Timeout
import be.cetic.tsorage.hub.auth.backend.AuthenticationBackend
import com.typesafe.config.ConfigFactory
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs.{Consumes, POST, Path, Produces}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.io.StdIn
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
@Path("/auth")
@Consumes(value = Array("application/json"))
@Produces(value = Array("application/json"))
class AuthenticationService(implicit executionContext: ExecutionContext) extends Directives with MessageJsonSupport
{
   val conf = ConfigFactory.load("auth.conf")
   val authenticator = AuthenticationBackend(conf.getConfig("backend"))

   @POST
   @Operation(
      summary = "Authenticates a user",
      description = "Returns a representation of the user if the submitted token is valid, or nothing otherwise",
      tags = Array("Authentication"),
      parameters = Array(
         new Parameter(
            name = "api_key",
            description = "A key token",
            example = "4b8639ed-0e90-4b3f-8a45-e87c22d17887",
            in = ParameterIn.QUERY
         )
   /*      new Parameter(
            name="test2",
            in = ParameterIn.DEFAULT,
            style=ParameterStyle.FORM
         ) */
      ),

      responses = Array(
         new ApiResponse(
            responseCode = "200",
            description = "A representation of the user associated with the submitted token",
            content = Array(new Content(schema = new Schema(implementation = classOf[AuthenticationResponse])))
         ),
         new ApiResponse(
            responseCode = "500",
            description = "Internal server error"
         ),
         new ApiResponse(
            responseCode = "400",
            description = "The request content was malformed"
         )
      )
   )
   def postAuth = path("auth") {
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
