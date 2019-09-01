package be.cetic.tsorage.ingestion.http

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import be.cetic.tsorage.ingestion.message.{FloatBody, FloatMessageJsonSupport}
import be.cetic.tsorage.ingestion.sink.MockupSink

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
      implicit val system = ActorSystem("http-interface")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher

      val sink = MockupSink


      val route = decodeRequest
      {
         withoutSizeLimit
         {
            path("api" / "v1" / "series")
            {
               post
               {
                  parameter('api_key)
                  {
                     api_key =>
                     {
                        println(s"bee bop ${api_key}")
                        println(decodeRequest)

                        entity(as[FloatBody])
                        { body =>
                           val messages = body.series.map(s => s.prepared())
                           messages foreach sink.submit

                           complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "OK"))
                        }
                     }
                  }
               }
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
