package be.cetic.tsorage.processor.consumer

import java.time.Duration
import java.util
import scala.collection.JavaConverters._

import be.cetic.tsorage.common.codec.Codec
import be.cetic.tsorage.common.messaging.Message
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.serialization.SerializationExtension
import akka.stream.ActorMaterializer
import be.cetic.tsorage.processor.consumer.Consumer.{consumer, i}
import com.google.protobuf.util.raw.SENSOR
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}
import spray.json.JsValue


object Consumer1 {

  implicit val system = ActorSystem("http-interface")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val TOPIC = "test"
  var i =0 // to count the number of received messages

  val consumerSettings = ConsumerSettings(system, new StringDeserializer,  new ByteArrayDeserializer)
    .withBootstrapServers("localhost:9092").withGroupId("group.id").withClientId(TOPIC)
  //s"${conf.getString("kafka.host")}:${conf.getInt("kafka.port")}"
  val consumer = consumerSettings.createKafkaConsumer()
  consumer.subscribe(util.Collections.singletonList(TOPIC))

  def main(args: Array[String]): Unit = {

    while (true) {
      val records = consumer.poll(Duration.ofMillis(100))

      val codec = new Codec

      for (record <- records.asScala)
       {
         val sampleData: Message = codec.decode(record.value())
        i = i+ 1
         println("***************************************************************************")
         println(i +" <<<<<<<< " + sampleData)
      }
    }
  }

}
