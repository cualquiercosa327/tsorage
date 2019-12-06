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
import be.cetic.tsorage.hub.filter.MetricManager
import be.cetic.tsorage.hub.grafana.GrafanaService
import be.cetic.tsorage.hub.metric.MetricHttpService
import be.cetic.tsorage.hub.tag.TagHttpService
import be.cetic.tsorage.hub.ts.TimeSeriesService
import com.typesafe.config.ConfigFactory

import scala.io.StdIn


/**
 * The global entry point for all the services.
 */
object Site extends RouteConcatenation with Directives
{
   private val conf = ConfigFactory.load("hub.conf")
   private implicit var system: ActorSystem = _
   private var database: TestDatabase = _

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

   private def onShutdown(): Unit = {
      database.clean()

      system.terminate()
   }

   def main(args: Array[String]): Unit =
   {
      //implicit val system = ActorSystem("authentication")
      system = ActorSystem("authentication")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher

      // Create a test database.
      database = new TestDatabase() // TODO: use a real database for production.
      database.clean()
      database.create()

      // Create the database handler.
      val databaseConf = ConfigFactory.load("test.conf") // TODO: change "test.conf" to "common.conf"
      val hubConf = ConfigFactory.load("hub.conf")
      val cassandra = new Cassandra(databaseConf)

      val authRoute = new AuthenticationService().route
      val metricRoutes = new MetricHttpService(cassandra).routes
      val tagRoutes = new TagHttpService(cassandra).routes
      val tsRoutes = new TimeSeriesService(cassandra).routes

      val grafanaRoutes = new GrafanaService(cassandra, MetricManager(cassandra, databaseConf)).routes

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
         tagRoutes ~
         tsRoutes
      )

      val bindingFuture = Http().bindAndHandle(routes, "localhost", conf.getInt("port"))

      scala.sys.addShutdownHook{
         onShutdown()
      }

      println(s"Server online at http://localhost:${conf.getInt("port")}/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
         .flatMap(_.unbind()) // trigger unbinding from the port
         .onComplete(_ => {
            Site.onShutdown()
         }) // and shutdown when done
   }
}
