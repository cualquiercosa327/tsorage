package be.cetic.tsorage.processor.consumer

//package scalapb
import java.time.Duration
import java.util

import com.google.protobuf.util.raw.SENSOR
import org.apache.kafka.common.serialization.ByteArrayDeserializer
//import akka.kafka.ProducerSettings
import akka.actor.ActorSystem
import akka.kafka.ConsumerSettings
import akka.stream.ActorMaterializer
import org.apache.kafka.common.serialization.StringDeserializer

import scala.collection.JavaConverters._


object Consumer {

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
  //consumer.subscribe(util.C)

  //val sampleData = SENSOR().withMetric(getMetric).withTagset(tagset).withV(values)
 val sam = SENSOR().metric
  //println (sam)
  def main(args: Array[String]): Unit = {
    while (true) {
      val records = consumer.poll(Duration.ofMillis(100))

      for (record <- records.asScala) {
        //val sampleData : String = SENSOR.Values.toString//Values(record.value().toString).toProtoString//.Values.()toString
        val sampleData : String = SENSOR.parseFrom(record.value()).toString
        i = i+ 1
       println(i +" <<<<<<<< " + sampleData)
      }
    }
  }
}
