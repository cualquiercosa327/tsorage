package be.cetic.tsorage.ingestion.http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{as, complete, entity}
import akka.http.scaladsl.server.Route
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.User
import be.cetic.tsorage.ingestion.message.{CheckRunMessageJsonSupport, DatadogBody, TSBody, TSMessageJsonSupport}
import org.apache.kafka.clients.producer.{Producer, ProducerRecord}
import spray.json._


/**
 * A HTTP Interface for the TSorage message format.
 */
class TSHTTPInterface extends HTTPInterface
   with TSMessageJsonSupport
   with CheckRunMessageJsonSupport
{
   override protected def processSeriesEntity(user: User, kafkaProducer: Producer[String, String]): Route = {
      entity(as[TSBody])
      { body =>
         val messages = body.series
         logger.debug(s"Received message: ${messages}")

         messages.map(message => new ProducerRecord[String, String](conf.getString("kafka.topic"), message.toJson.compactPrint))
            .foreach(pr => kafkaProducer.send(pr))

         complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "OK"))
      }
   }
}
