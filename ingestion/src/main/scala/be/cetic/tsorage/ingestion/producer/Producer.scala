package be.cetic.tsorage.ingestion.producer
//package scalapb


import be.cetic.tsorage.common
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.serialization.SerializationExtension
import akka.stream.ActorMaterializer
import com.google.protobuf.util.raw.SENSOR
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import play.api.libs.json.{JsArray, JsValue, Json}
import scala.io.Source

object Producer {
  ///////////// Define System Setting
  implicit val system = ActorSystem("http-interface")
  val serialization = SerializationExtension(system)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {

    ///////////// Define Producer Setting
    val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers("localhost:9092")
    //s"${conf.getString("kafka.host")}:${conf.getInt("kafka.port")}"
    val kafkaProducer = producerSettings.createKafkaProducer()
    val TOPIC = "test"
    var i =0 // to count the number of sent messages

    //////////////////////////////////// importing Json file, convert lines to json objects, and Define Message Format over Protobuf

    try {
      val lines = Source.fromFile("ingestion/src/main/kafka-raw.sample")  // use this to sent all message from sampleData
      //val lines = io.Source.fromFile("src/main/kafka-raw2.txt")  // use this to sent sample message with different Values size

      for (line <- lines.getLines ) {
        val parsed: JsValue = Json.parse(line)

        val metric: String =(parsed \ "metric").asOpt[String].getOrElse("")

        val tag_version: String = (parsed \ "tagset" \ "version").asOpt[String].getOrElse("")
        val tag_type: String = (parsed \"tagset" \ "type").asOpt[String].getOrElse("")
        val tag_interval: String = (parsed \"tagset" \  "interval").asOpt[String].getOrElse("")
        val tag_host: String = (parsed \"tagset" \ "host").asOpt[String].getOrElse("")
        val tagset = SENSOR.TAGSET().withVersion(tag_version).withType(tag_type).withInterval(tag_interval).withHost(tag_host) // prepare tagset to protobuf message

        var valDate :String = ""
        var valMeasure:Double = 0

        val p: Int =   ((parsed \\ "values").headOption match { // to count the number of values
          case Some(JsArray(values)) => values.length
          case _ => 0 })

        var Val: Seq[SENSOR.Values] = Seq.empty[SENSOR.Values]
        for (x <- 0 to p-1)
        {
          for (y <- 0 to 1)
          {
            if (y == 0) valDate = (parsed \ "values" \ x \ y).asOpt[String].getOrElse("")
            else valMeasure = (parsed \ "values" \ x \ y).asOpt[Double].getOrElse(0)
          }
          val values = SENSOR.Values().withDateTime(valDate).withMeasure(valMeasure) //  prepare values to protobuf message
          Val =  Val :+  values
        }

        val sampleData = SENSOR().withMetric(metric).withTagset(tagset).addAllV(Val) // prepare protobuf message
        //println (sampleData)
        val record: ProducerRecord[String, Array[Byte]] = new ProducerRecord("test", sampleData.toByteArray) // envlope the message to Kafka sender
        //println(record)
        kafkaProducer.send(record)
        i = i+ 1
        println(i +">>>>>>>" + s"sending $record")
      }
    }
    finally
    {
      kafkaProducer.flush()
      kafkaProducer.close()
    }
  }
}
