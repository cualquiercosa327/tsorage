package be.cetic.tsorage.processor.source

import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Source
import be.cetic.tsorage.processor.Message
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import spray.json.{RootJsonFormat, _}

object KafkaConsumer {
  val kafkaConfig = ConfigFactory.load("tsorage.conf").getConfig("kafka")
  val bootstrapServerUrl = s"${kafkaConfig.getString("host")}:${kafkaConfig.getString("port")}"
  val group = kafkaConfig.getString("tsorage-processor")

  val config = ConfigFactory.load("kafka-consumer.conf").getConfig("akka.kafka.consumer")

  val consumerSettings =
    ConsumerSettings(config, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServerUrl)
      .withGroupId(group)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

}


class KafkaConsumer() extends DefaultJsonProtocol {

  private val config = ConfigFactory.load("tsorage.conf")
  private val topic = config.getString("kafka.topic")
  private val subscription = Subscriptions.topics(topic)
  private val consumerSettings = KafkaConsumer.consumerSettings

  val source: Source[ConsumerRecord[String, Array[Byte]], Consumer.Control] = Consumer.plainSource(consumerSettings, subscription)

  def deserializedSource(): Source[Message, Consumer.Control] = {
    source.map { consumerRecord =>
      val value: Array[Byte] = consumerRecord.value()
      val data = Message.messageFormat.read(value.toJson)
      data
    }
  }
}
