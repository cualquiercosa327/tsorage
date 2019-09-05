package be.cetic.tsorage.ingestion.http

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import be.cetic.tsorage.ingestion.message.{CheckRunMessage, FloatBody, FloatMessageJsonSupport}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

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

      val conf = ConfigFactory.load("ingest-http.conf")
      val config = system.settings.config.getConfig("akka.kafka.producer")

      val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
         .withBootstrapServers(s"${conf.getString("kafka.host")}:${conf.getInt("kafka.port")}")

      val kafkaProducer = producerSettings.createKafkaProducer()

      val routeSeries =
      decodeRequest
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
                        entity(as[FloatBody])
                        { body =>
                           val messages = body.series.map(s => s.prepared())

                           messages.map(message => new ProducerRecord[String, String](conf.getString("kafka.topic"), message.toJson.compactPrint))
                                 .foreach(pr => kafkaProducer.send(pr))

                           complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "OK"))
                        }
                     }
                  }
               }
            }
         }
      }

      val routeCheckRun = decodeRequest
      {
         withoutSizeLimit
         {
            path("api" / "v1" / "check_run")
            {
               post
               {
                  parameter('api_key)
                  {
                     api_key =>
                     {
                        entity(as[List[CheckRunMessage]])
                        { body =>
                           complete(HttpEntity(ContentTypes.`application/json`, """{"status": "ok"}"""))
                           // TODO: Determine a proper way to process this kind of message
                        }
                     }
                  }
               }
            }
         }
      }


      val bindingFuture = Http().bindAndHandle(concat(routeSeries, routeCheckRun), "localhost", 8080)

      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
         .flatMap(_.unbind()) // trigger unbinding from the port
         .onComplete(_ => {
            kafkaProducer.close()
            system.terminate()
         }) // and shutdown when done
   }
}
