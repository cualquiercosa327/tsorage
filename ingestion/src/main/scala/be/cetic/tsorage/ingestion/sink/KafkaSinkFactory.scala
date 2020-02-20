package be.cetic.tsorage.ingestion.sink
import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorMaterializer, SinkShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.common.messaging.message.MessagePB
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{BytesSerializer, StringSerializer}
import org.apache.kafka.common.utils.Bytes

import scala.concurrent.ExecutionContextExecutor
import collection.JavaConverters._
import GraphDSL.Implicits._


/**
 * A factory for the Kafka Sink.
 */
object KafkaSinkFactory extends SinkFactory
{
   override def createSink(config: Config)
                          (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContextExecutor): Sink[Message, _] =
   {
      Sink.fromGraph(createGraph(config))
   }

   private def createGraph(config: Config)(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContextExecutor) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>
         val topic = config.getString("topic")

         val bootstrapServers = config.getConfigList("nodes").asScala.toList
            .map(nodeConf => s"${nodeConf.getString("host")}:${nodeConf.getInt("port")}")
            .mkString(",")

         val kafkaProducerSettings = ProducerSettings(system, new StringSerializer, new BytesSerializer)
            .withBootstrapServers(bootstrapServers)

         val sink = builder.add(Producer.plainSink(kafkaProducerSettings))

         val asKafkaMessage = builder.add(
               Flow[Message].map(msg => {
               val value = new Bytes(Message.asPB(msg).toByteArray)
               new ProducerRecord[String, Bytes](topic, msg.metric, value)
            })
         )

         asKafkaMessage ~> sink

         SinkShape(asKafkaMessage.in)
   }
}