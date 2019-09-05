package be.cetic.tsorage.hub.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{as, complete, concat, decodeRequest, entity, parameter, path, post, withoutSizeLimit}
import akka.stream.ActorMaterializer
import be.cetic.tsorage.hub.auth.backend.AuthenticationBackend
import com.typesafe.config.ConfigFactory
import spray.json._


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
object AuthenticationService extends MessageJsonSupport
{
   def main(args: Array[String]): Unit =
   {
      implicit val system = ActorSystem("authentication")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher

      val conf = ConfigFactory.load("auth.conf")
      val authenticator = AuthenticationBackend(conf.getConfig("backend"))

      val route =

         path("auth")
         {
            post
            {
                  entity(as[AuthenticationQuery])
                  {
                     query => complete(HttpEntity(ContentTypes.`application/json`, authenticator.check(query.api_key).toJson.compactPrint))
                  }
            }
         }

      val bindingFuture = Http().bindAndHandle(route, "localhost", conf.getInt("port"))

      println(s"Server online at http://localhost:${conf.getInt("port")}/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
         .flatMap(_.unbind()) // trigger unbinding from the port
         .onComplete(_ => {
            system.terminate()
         }) // and shutdown when done
   }
}
