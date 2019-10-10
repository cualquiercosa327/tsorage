package be.cetic.tsorage.hub

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post}
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{Directives, Route, RouteConcatenation}
import akka.stream.ActorMaterializer
import be.cetic.tsorage.hub.auth.{AuthenticationQuery, AuthenticationService}
import be.cetic.tsorage.hub.auth.backend.AuthenticationBackend
import be.cetic.tsorage.hub.grafana.{FakeDatabase, GrafanaService}
import be.cetic.tsorage.hub.metric.MetricHttpService
import com.typesafe.config.ConfigFactory

import scala.io.StdIn

/**
 * The global entry point for all the services.
 */
object Site extends RouteConcatenation with Directives
{
   // Route to test the connection with the server.
   val connectionTestRoute: Route = path("") {
      get {
         DebuggingDirectives.logRequestResult("Connection test route (/)", Logging.InfoLevel) {
            complete(StatusCodes.OK)
         }
      }
   }

   val swaggerRoute: Route = path("swagger") { getFromResource("swagger-ui/index.html") } ~
     getFromResourceDirectory("swagger-ui") ~
     pathPrefix("api-docs") { getFromResourceDirectory("api-docs") }

   def main(args: Array[String]): Unit =
   {
      implicit val system = ActorSystem("authentication")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher

      val conf = ConfigFactory.load("auth.conf")

      val authRoute = new AuthenticationService().route
      val metricRoutes = new MetricHttpService().routes
      val grafanaRoutes = new GrafanaService(new FakeDatabase()).routes // TODO: to be changed by a real Cassandra database.

      val routes = (authRoute ~ metricRoutes ~ grafanaRoutes ~ connectionTestRoute ~ swaggerRoute)

      val bindingFuture = Http().bindAndHandle(routes, "localhost", conf.getInt("port"))

      println(s"Server online at http://localhost:${conf.getInt("port")}/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
         .flatMap(_.unbind()) // trigger unbinding from the port
         .onComplete(_ => {
            system.terminate()
         }) // and shutdown when done
   }
}
