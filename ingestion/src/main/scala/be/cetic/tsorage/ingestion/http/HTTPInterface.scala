package be.cetic.tsorage.ingestion.http

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection, Directive1}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{PredefinedFromEntityUnmarshallers, Unmarshal, Unmarshaller}
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import be.cetic.tsorage.ingestion.message.{AuthenticationQuery, CheckRunMessage, DoubleBody, DoubleMessage, FloatMessageJsonSupport, User}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.io.StdIn
import spray.json._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
 * An AKKA system that runs an HTTP server waiting for Datadog compliant messages.
 * It implements a part of the Datadog Metrics API : https://docs.datadoghq.com/api/?lang=python#post-timeseries-points
 */
object HTTPInterface extends FloatMessageJsonSupport with DefaultJsonProtocol
{
   implicit val system = ActorSystem("http-interface")
   implicit val materializer = ActorMaterializer()
   implicit val executionContext = system.dispatcher

   def verifyToken(conf: Config)(token: String): Future[Option[User]] = {
      val request = HttpRequest(
         method = HttpMethods.POST,
         uri = s"${conf.getString("authentication.host")}:${conf.getInt("authentication.port")}${conf.getString("authentication.path")}",
         entity = HttpEntity(ContentTypes.`application/json`, AuthenticationQuery(token).toJson.compactPrint)
      )

      val response = Http(system).singleRequest(request).map(response => response.entity)

      response.flatMap(r => Unmarshal(r).to[Option[User]])
   }

   def authorize(conf: Config, system: ActorSystem, context: ExecutionContext, mat: ActorMaterializer)(token: String): Directive1[User] =
   {
      onComplete(verifyToken(conf)(token)).flatMap {
         case Success(Some(u)) => provide(u)
         case _ => reject(AuthorizationFailedRejection)
      }
   }

   def main(args: Array[String]): Unit =
   {
      val conf = ConfigFactory.load("ingest-http.conf")

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
                     authorize(conf, system, executionContext, materializer)(api_key){
                        user =>

                           entity(as[DoubleBody])
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
