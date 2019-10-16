package be.cetic.tsorage.hub

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{Directives, Route, RouteConcatenation}
import akka.stream.ActorMaterializer
import be.cetic.tsorage.common.Cassandra
import be.cetic.tsorage.hub.auth.AuthenticationService
import be.cetic.tsorage.hub.grafana.{FakeDatabase, GrafanaService}
import be.cetic.tsorage.hub.metric.MetricHttpService
import be.cetic.tsorage.hub.tag.TagHttpService
import com.typesafe.config.ConfigFactory

import scala.io.StdIn


/**
 * The global entry point for all the services.
 */
object Site extends RouteConcatenation with Directives
{
   private val conf = ConfigFactory.load("hub.conf")

   // Route to test the connection with the server.
   val connectionTestRoute: Route = path("api" / conf.getString("api.version")) {
      get {
         DebuggingDirectives.logRequestResult(s"Connection test route (${conf.getString("api.prefix")})", Logging.InfoLevel) {
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

      val authRoute = new AuthenticationService().route
      val metricRoutes = new MetricHttpService().routes
      val tagRoutes = new TagHttpService().routes

      val grafanaRoutes = new GrafanaService(new Cassandra(ConfigFactory.load("test.conf"))).routes // TODO: change "test.conf" to "common.conf"

      // Route to test the connection with the server.
      val testConnectionRoute = path("") {
         get {
            DebuggingDirectives.logRequestResult("Connection test route (/)", Logging.InfoLevel) {
               complete(StatusCodes.OK)
            }
         }
      }


      val swaggerRoute = path("swagger") { getFromResource("swagger-ui/index.html") } ~
         getFromResourceDirectory("swagger-ui") ~
         pathPrefix("api-docs") { getFromResourceDirectory("api-docs") }

      val routes = (
         authRoute ~
         metricRoutes ~
         grafanaRoutes ~
         connectionTestRoute ~
         swaggerRoute ~
         tagRoutes
      )

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
