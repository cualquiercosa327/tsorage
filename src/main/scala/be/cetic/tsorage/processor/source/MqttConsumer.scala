package be.cetic.tsorage.processor.source

import akka.Done
import akka.stream.alpakka.mqtt.scaladsl.{MqttMessageWithAck, MqttSource}
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json.{RootJsonFormat, _}

import scala.concurrent.Future

object MqttConsumer {

  private val config = ConfigFactory.load("storage.conf")
  private val broker = s"${config.getString("mqtt.broker")}:${config.getString("mqtt.port")}"
  private val clientId = config.getString("mqtt.clientId")

  val qos: MqttQoS = config.getString("mqtt.qos") match {
    case "atMostOnce" => MqttQoS.atMostOnce
    case "atLeastOnce" => MqttQoS.atLeastOnce
    case "exactlyOnce" => MqttQoS.exactlyOnce
  }

  val bufferSize: Int = config.getInt("mqtt.buffer-size")

  val topic: String = config.getString("mqtt.topic")

  val connectionSettings = MqttConnectionSettings(broker, clientId, new MemoryPersistence)

}

class MqttConsumer extends DefaultJsonProtocol {
  private val subscription = MqttSubscriptions(MqttConsumer.topic, MqttConsumer.qos)
  val source: Source[MqttMessageWithAck, Future[Done]] = MqttSource.atLeastOnce(MqttConsumer.connectionSettings, subscription, bufferSize = MqttConsumer.bufferSize)

  def deserializedSource[T]()(implicit deserializer: RootJsonFormat[T]): Source[T, Future[Done]] = {
   val typedSource = source.map { mqttMessageWithAck =>
      val data = mqttMessageWithAck.message.payload.toArray.toJson.convertTo[T]
      data
    }
    typedSource
  }

}
