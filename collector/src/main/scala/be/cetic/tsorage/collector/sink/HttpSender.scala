package be.cetic.tsorage.collector.sink

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.collector.MessageSender
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.common.messaging.message.MessagePB
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import spray.json._

/**
 * Sink for sending data points to the HTTP component of the ingestion layer.
 */
object HttpSender extends MessageSender
   with MessageJsonSupport
   with LazyLogging
{
   private def process(uri: Uri)(crr: CommittableReadResult)(implicit ec: ExecutionContext, system: ActorSystem): Future[CommittableReadResult] =
   {
      val decoded: Message = Message.fromPB(MessagePB.parseFrom(crr.message.bytes.toArray))
      logger.debug(s"Message to submit: ${decoded}")

      val responseFuture: Future[HttpResponse] = Http().singleRequest(
         HttpRequest(
            uri = uri,
            method = HttpMethods.POST,
            entity = HttpEntity(
               ContentTypes.`application/json`,
               JsObject(Map("series" -> JsArray(decoded.toJson))).compactPrint
            )
         )
      )

      responseFuture.map(rf => crr)
   }

   /**
    * Creates a flow consisting in sending commitable read result, and
    * forwarding them as an output. The result must is emitted only once the sender
    * knows it has been taken into account.
    */
   def buildSender(config: Config)(implicit ec: ExecutionContext, system: ActorSystem)
   : Flow[CommittableReadResult, CommittableReadResult, NotUsed] =
   {
      val uri = {
         val ip = config.getString("ip")
         val port = config.getInt("port")
         val prefix = config.getString("prefix")
         val api_key = config.getString("api_key")

         Uri(s"http://${ip}:${port}/${prefix}?api_key=${api_key}")
      }

      Flow[CommittableReadResult].mapAsyncUnordered(4)(process(uri))
   }
}
