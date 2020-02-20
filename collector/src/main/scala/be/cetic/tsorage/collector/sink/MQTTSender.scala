package be.cetic.tsorage.collector.sink

import java.util.UUID

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp.scaladsl.CommittableReadResult
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}
import akka.util.ByteString
import be.cetic.tsorage.collector.MessageSender
import GraphDSL.Implicits._
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.alpakka.mqtt.scaladsl.{MqttMessageWithAck, MqttSink}
import akka.stream.{FlowShape, Graph}
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.common.messaging.message.MessagePB
import com.typesafe.config.Config
import javax.net.ssl.SSLContext
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Sink for sending messages to the MQTT ingestion service.
 */
object MQTTSender extends MessageSender with MessageJsonSupport
{
   /**
    * Builds a flow that sends bufferized messages represented as committable read results.
    *
    * @param config The message to send.
    * @return A flow of bufferized messages.
    */
   override def buildSender(config: Config)
   (implicit ec: ExecutionContext, system: ActorSystem)
   : Flow[CommittableReadResult, CommittableReadResult, NotUsed] =
      Flow.fromGraph(buildGraph(config))


   private def buildGraph(config: Config): Graph[FlowShape[CommittableReadResult, CommittableReadResult], NotUsed] = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val uriPrefix = config.getString("security.type") match {
            case "ssl" => "ssl"
            case _ => "tcp"
         }

         val uri = s"${uriPrefix}://${config.getString("host")}:${config.getInt("port")}"
         val clientId = config.getString("client_id") match {
            case "" => UUID.randomUUID().toString
            case id: String => id
         }

         val topic = config.getString("channel")

         val basicSettings: MqttConnectionSettings = MqttConnectionSettings(
            uri,
            clientId,
            new MemoryPersistence
         )

         val advancedSettings: MqttConnectionSettings = config.getString("security.type") match {
            case "anonymous" => basicSettings
            case "password" => basicSettings
               .withAuth(config.getString("security.login"), config.getString("security.password"))
            case "ssl" => basicSettings
               .withAuth(config.getString("security.login"), config.getString("security.password"))
               .withSocketFactory(SSLContext.getDefault.getSocketFactory)
         }

         val sink = MqttSink(advancedSettings, MqttQoS.AtLeastOnce)

         val asBytes = builder.add(
            config.getString("type") match {
               case "mqtt/pb" => Flow[CommittableReadResult]
                  .map(crr => crr.message.bytes)
               case "mqtt/json" => Flow[CommittableReadResult]
                  .map(crr => ByteString(
                        Message
                        .fromPB(MessagePB.parseFrom(crr.message.bytes.toArray) )
                        .toJson
                        .compactPrint
                        .getBytes("utf8")
                     )
                  )
            }
         )

         val asMqttMsg = builder.add(
            Flow[ByteString].map(bs => MqttMessage(topic, bs))
         )

         val broadcast = builder.add(Broadcast[CommittableReadResult](2))

         broadcast.out(0) ~> asBytes ~> asMqttMsg ~> sink

         FlowShape(broadcast.in, broadcast.out(1))
   }
}
