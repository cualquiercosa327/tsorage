package be.cetic.tsorage.processor.source

import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import spray.json.{RootJsonFormat, _}

object KafkaConsumer {
  val kafkaConfig = ConfigFactory.load("storage.conf").getConfig("kafka")
  val bootstrapServerUrl = s"${kafkaConfig.getString("host")}:${kafkaConfig.getString("port")}"
  val group = kafkaConfig.getString("group")

  val config = ConfigFactory.load("kafka-consumer.conf").getConfig("akka.kafka.consumer")

  val consumerSettings =
    ConsumerSettings(config, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServerUrl)
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

}


class KafkaConsumer() extends DefaultJsonProtocol {

  private val config = ConfigFactory.load()
  private val topic = config.getString("kafka.topic")
  private val subscription = Subscriptions.topics(topic)
  private val consumerSettings = KafkaConsumer.consumerSettings

  val source: Source[ConsumerRecord[String, Array[Byte]], Consumer.Control] = Consumer.plainSource(consumerSettings, subscription)

  def deserializedSource[T]()(implicit deserializer: RootJsonFormat[T]): Source[T, Consumer.Control] = {
    source.map { consumerRecord =>
      val value: Array[Byte] = consumerRecord.value()
      val data = value.toJson.convertTo[T]
      data
    }
  }
}
