package be.cetic.tsorage.ingestion.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import be.cetic.tsorage.ingestion.message.{FloatBody, FloatMessageJsonSupport}

import scala.io.StdIn

import spray.json._

/**
 * An AKKA system that runs an HTTP server waiting for Datadog compliant messages.
 * It implements a part of the Datadog Metrics API : https://docs.datadoghq.com/api/?lang=python#post-timeseries-points
 */
object HTTPInterface extends FloatMessageJsonSupport
{
   def main(args: Array[String]): Unit =
   {
      implicit val system = ActorSystem("my-system")
      implicit val materializer = ActorMaterializer()
      // needed for the future flatMap/onComplete in the end
      implicit val executionContext = system.dispatcher

      val route =
         path("api" / "v1" / "series") {
            post {
               entity(as[FloatBody]) { body =>
                  val messages = body.series.map(s => s.prepared())
                  messages.foreach(m => println(m.toJson))
                  complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Say hello to akka-http ${messages}</h1>"))
               }
            }
         }

      val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
         .flatMap(_.unbind()) // trigger unbinding from the port
         .onComplete(_ => system.terminate()) // and shutdown when done
   }
}
