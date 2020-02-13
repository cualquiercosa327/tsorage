package be.cetic.tsorage.ingestion.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.{AuthenticationQuery, User}
import be.cetic.tsorage.ingestion.IngestionConfig
import be.cetic.tsorage.ingestion.message.{CheckRunMessage, DatadogBody, DatadogMessageJsonSupport}
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.serialization.StringSerializer
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * An AKKA system that runs an HTTP server waiting for Datadog compliant messages.
 * It implements a part of the Datadog Metrics API : https://docs.datadoghq.com/api/?lang=python#post-timeseries-points
 */
object HTTPInterface extends DatadogMessageJsonSupport
   with DefaultJsonProtocol
   with MessageJsonSupport
{
   implicit val system = ActorSystem("http-interface")
   implicit val materializer = ActorMaterializer()
   implicit val executionContext = system.dispatcher

   private val conf = IngestionConfig.conf

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
      val kafkaHost = conf.getString("kafka.host")
      val kafkaPort = conf.getInt("kafka.port")
      val kafkaTopic = conf.getString("kafka.topic")

      val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
        .withBootstrapServers(s"$kafkaHost:$kafkaPort")

      val kafkaProducer = Try {
         producerSettings.createKafkaProducer()
      } match {
         case Failure(_:KafkaException) =>
            Console.err.println(s"Cannot connect to the Kafka host: $kafkaHost:$kafkaPort.")
            sys.exit(1)
         case Failure(e) =>
            Console.err.println(s"Unexpected error has occurred when tried to connect to Kafka host:\n$e")
            sys.exit(2)
         case Success(value) => value
      }

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

                           entity(as[DatadogBody])
                           { body =>
                              val messages = body.series.map(ddMsg => ddMsg.prepare(user))

                              messages.map(message => new ProducerRecord[String, String](kafkaTopic, message.toJson.compactPrint))
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

      val hubListenAddress = System.getenv().getOrDefault("TSORAGE_INGESTION_LISTEN_ADDRESS", "localhost")
      val bindingFuture = Http().bindAndHandle(concat(routeSeries, routeCheckRun), hubListenAddress, 8080)

      scala.sys.addShutdownHook {
         println("Shutdown...")

         bindingFuture
           .flatMap(_.unbind()) // trigger unbinding from the port
           .onComplete(_ => {
              system.terminate()
           }) // and shutdown when done
      }
   }
}
