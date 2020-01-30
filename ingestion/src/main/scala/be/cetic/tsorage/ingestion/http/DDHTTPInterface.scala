package be.cetic.tsorage.ingestion.http

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{as, complete, entity}
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.User
import be.cetic.tsorage.ingestion.message.DatadogBody
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}
import spray.json._


/**
 * An AKKA system that runs an HTTP server waiting for Datadog compliant messages.
 * It implements a part of the Datadog Metrics API : https://docs.datadoghq.com/api/?lang=python#post-timeseries-points
 */
class DDHTTPInterface extends HTTPInterface
   with MessageJsonSupport
{
   override protected def processSeriesEntity(user: User, kafkaProducer: Producer[String, String]): server.Route =
   {
      entity(as[DatadogBody])
      { body =>
         val messages = body.series.map(ddMsg => ddMsg.prepare(user))
         logger.debug(s"Received message: ${messages}")

         messages.map(message => new ProducerRecord[String, String](conf.getString("kafka.topic"), message.toJson.compactPrint))
            .foreach(pr => kafkaProducer.send(pr))

         complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "OK"))
      }
   }
}
