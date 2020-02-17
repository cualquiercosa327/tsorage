package be.cetic.tsorage.ingestion.source
import java.util.UUID

import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import com.google.protobuf.util.message.MessagePB
import com.typesafe.config.Config
import javax.net.ssl.SSLContext
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import spray.json._

/**
 *
 * mqtt sub -t timeseries -h localhost -p 1883 -u steve -pw password
 */
object MQTTSourceFactory extends SourceFactory with MessageJsonSupport
{
   override def createSource(config: Config): Source[Message, _] =
   {
      val channel = config.getString("channel")

      val uriPrefix = config.getString("security.type") match {
         case "ssl" => "ssl"
         case _ => "tcp"
      }

      val uri = s"${uriPrefix}://${config.getString("host")}:${config.getInt("port")}"

      val clientId: String = config.getString("client_id") match {
         case "" => UUID.randomUUID().toString
         case id: String => id
      }

      val basicSettings: MqttConnectionSettings = MqttConnectionSettings(uri, clientId, new MemoryPersistence)


      val advancedSettings: MqttConnectionSettings = config.getString("security.type") match {
         case "anonymous" => basicSettings
         case "password" => basicSettings
            .withAuth(config.getString("security.login"), config.getString("security.password"))
         case "ssl" => basicSettings
            .withAuth(config.getString("security.login"), config.getString("security.password"))
            .withSocketFactory(SSLContext.getDefault.getSocketFactory)
      }

      val mqttSource: Source[MqttMessage, Future[Done]] =
         MqttSource.atMostOnce(
            advancedSettings,
            MqttSubscriptions(channel, MqttQoS.exactlyOnce),
            bufferSize = 8
         )

      Source.fromGraph(
         mqttSource
            .map(mqttMsg => {
               config.getString("type") match {
                  case encoder if encoder endsWith "/pb" => Message.fromPB(MessagePB.parseFrom(mqttMsg.payload.toArray))
                  case encoder if encoder endsWith "/json" => mqttMsg.payload.utf8String.parseJson.convertTo[Message]
               }
            })
      )
   }
}
