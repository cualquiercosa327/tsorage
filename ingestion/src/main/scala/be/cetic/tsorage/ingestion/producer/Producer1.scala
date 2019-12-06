

package be.cetic.tsorage.ingestion.producer
//package scalapb
import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.serialization.SerializationExtension
import akka.stream.ActorMaterializer
import be.cetic.tsorage.common.codec.Codec
import be.cetic.tsorage.common.messaging.Message
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import spray.json.DefaultJsonProtocol._
import spray.json._
object Producer1 {

  implicit val system = ActorSystem("http-interface")
  val serialization = SerializationExtension(system)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {

    val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers("localhost:9092")
    //s"${conf.getString("kafka.host")}:${conf.getInt("kafka.port")}"
    val kafkaProducer = producerSettings.createKafkaProducer()
    val TOPIC = "test"

    // a Message values for testing the Codec Class
    var msg = new Message(
      metric =  "datadog.trace_agent.trace_writer.errors" ,
      tagset = Map("version"->"6.13.0","type"->"rate","interval"->"10","host"->"macbook-mg.local"),
      `type` = "rate",
      values = List((LocalDateTime.now(),{0.77777}.toJson),(LocalDateTime.now(),{0.154215}.toJson))
    )


    var codec = new Codec
    var a = codec.encode(msg)
    println("***************************************************************************")
    println(msg)
    val record: ProducerRecord[String, Array[Byte]] = new ProducerRecord("test", a)
    kafkaProducer.send(record)

    println(">>>>>>>" + s"sending $record")
    kafkaProducer.flush()
    kafkaProducer.close()
  }
}







