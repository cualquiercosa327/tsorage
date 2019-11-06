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
import com.typesafe.config.ConfigFactory

/**
 * The global entry point for all the services.
 */
object Site extends RouteConcatenation with Directives
{
   private val conf = ConfigFactory.load("hub.conf")

   // Route to test the connection with the server.
   val connectionTestRoute: Route = path("api" / conf.getString("api.version")) {
      get {
         DebuggingDirectives.logRequestResult(s"Connection test route (${conf.getString("api.prefix")})",
            Logging.InfoLevel) {
            complete(StatusCodes.OK)
         }
      }
   }

   // Route to documentation.
   val swaggerRoute: Route = path("swagger") { getFromResource("swagger-ui/index.html") } ~
     getFromResourceDirectory("swagger-ui") ~
     pathPrefix("api-docs") { getFromResourceDirectory("api-docs") }

   def main(args: Array[String]): Unit =
   {
      //implicit val system = ActorSystem("authentication")
      implicit val system: ActorSystem = ActorSystem("authentication")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher

      // Create a test database.
      val database: TestDatabase = new TestDatabase() // TODO: use a real database for production.
      database.create()

      // Create the database handler.
      val databaseConf = ConfigFactory.load("test.conf") // TODO: change "test.conf" to "common.conf"
      val cassandra = new Cassandra(databaseConf)

      // Routes.
      val authRoute = new AuthenticationService().route
      val metricRoutes = new MetricHttpService(cassandra).routes
      val tagRoutes = new TagHttpService(cassandra).routes
      val grafanaRoutes = new GrafanaService(cassandra, MetricManager(cassandra, databaseConf)).routes

      val routes =
         authRoute ~
           metricRoutes ~
           grafanaRoutes ~
           connectionTestRoute ~
           swaggerRoute ~
           tagRoutes

      val bindingFuture = Http().bindAndHandle(routes, "localhost", conf.getInt("port"))

      scala.sys.addShutdownHook{
         println("Shutdown...")

         database.clean()

         bindingFuture
           .flatMap(_.unbind()) // trigger unbinding from the port
           .onComplete(_ => {
              system.terminate()
           }) // and shutdown when done
      }
   }
}
