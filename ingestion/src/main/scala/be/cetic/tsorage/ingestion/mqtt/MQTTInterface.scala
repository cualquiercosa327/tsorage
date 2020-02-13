package be.cetic.tsorage.ingestion.mqtt

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorMaterializer, KillSwitches, OverflowStrategy, UniqueKillSwitch}
import akka.stream.alpakka.mqtt.streaming.{Command, ConnAck, ConnAckFlags, ConnAckReturnCode, Connect, ControlPacketFlags, Event, MqttCodec, MqttSessionSettings, PubAck, Publish, SubAck, Subscribe}
import akka.stream.alpakka.mqtt.streaming.scaladsl.{ActorMqttServerSession, Mqtt}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Sink, Source, Tcp}
import akka.util.ByteString
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.ingestion.IngestionConfig
import be.cetic.tsorage.ingestion.http.HTTPInterface.{conf, system}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{Future, Promise}
import spray.json._

import scala.util.{Failure, Success, Try}

/**
 * An interface for ingesting observations using MQTT.
 *
 * This implementation is based on a MQTT v. 3 server, without intermediary MQTT broker.
 *
 * Example of message submission from localhost:
 *
 * mqtt pub -h localhost -p 1883 --topic "timeseries" --message "{\"metric\": \"my-sensor\", \"tagset\": {\"source\": \"mqtt\", \"quality\": \"good\", \"owner\": \"nsa\"}, \"type\": \"tdouble\", \"values\": [[\"$(date +%Y-%m-%dT%H:%M:%S)\", 123.456],[\"$(date -v +5M "+%Y-%m-%dT%H:%M:%S")\", 789.123]]}" -V 3
 *
  for run in {1..10}
  do
     mqtt pub -h localhost -p 1883 --topic "timeseries" --message "{\"metric\": \"my-sensor\", \"tagset\": {\"source\": \"mqtt\", \"quality\": \"good\", \"owner\": \"nsa\"}, \"type\": \"tdouble\", \"values\": [[\"$(date +%Y-%m-%dT%H:%M:%S)\", 123.456],[\"$(date -v +5M "+%Y-%m-%dT%H:%M:%S")\", 789.123]]}" -V 3;
     sleep 5;
  done
 */
object MQTTInterface extends LazyLogging with MessageJsonSupport
{
   implicit val system = ActorSystem("mqtt-interface")
   implicit val materializer = ActorMaterializer()
   implicit val executionContext = system.dispatcher

   private val conf = IngestionConfig.conf
   private val commonConf = ConfigFactory.load("common.conf")

   def main(args: Array[String]): Unit =
   {
      val settings = MqttSessionSettings()
      val session = ActorMqttServerSession(settings)

     val kafkaHost = conf.getString("kafka.host")
     val kafkaPort = conf.getInt("kafka.port")
     val kafkaTopic = conf.getString("kafka.topic")

      val maxConnections = conf.getInt("mqtt.max_connections")
      val host = conf.getString("mqtt.host")
      val port = conf.getInt("mqtt.port")
      val bufferSize = conf.getInt("mqtt.buffer_size")
      val mqttChannel = conf.getString("mqtt.channel")

      val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
         .withBootstrapServers(s"$kafkaHost:$kafkaPort")

      val bindSource: Source[Either[MqttCodec.DecodeError, Event[Nothing]], Future[Tcp.ServerBinding]] =
         Tcp()
            .bind(host, port)
            .flatMapMerge(
               maxConnections, { connection =>
                  val mqttFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
                     Mqtt
                        .serverSessionFlow(session, ByteString(connection.remoteAddress.getAddress.getAddress))
                        .join(connection.flow)

                  val (queue, source) = Source
                     .queue[Command[Nothing]](bufferSize = bufferSize, OverflowStrategy.dropHead)
                     .via(mqttFlow)
                     .toMat(BroadcastHub.sink)(Keep.both)
                     .run()

                  val subscribed = Promise[Done]
                  source
                     .runForeach {
                        case Right(Event(_: Connect, _)) =>
                           queue.offer(Command(ConnAck(ConnAckFlags.None, ConnAckReturnCode.ConnectionAccepted)))
                        case Right(Event(cp: Subscribe, _)) =>
                           queue.offer(Command(SubAck(cp.packetId, cp.topicFilters.map(_._2)), Some(subscribed), None))
                        case Right(Event(publish @ Publish(flags, _, Some(packetId), _), _))
                           if flags.contains(ControlPacketFlags.RETAIN) =>
                           queue.offer(Command(PubAck(packetId)))

                           subscribed.future.foreach(_ => session ! Command(publish))
                        case _ => // Ignore everything else
                     }
                  source
               }
            )

      val graph = bindSource
         .viaMat(KillSwitches.single)(Keep.both)
         .map(value => value match {
            case Right(Event(Publish(_, mqttChannel, _, payload: ByteString), _)) => Some(payload.utf8String.parseJson)
            case _ => None
         })
         .filter(_.isDefined).map(_.get)
         .map(jvalue => Try(jvalue.convertTo[Message]).toEither.left.map(t => {logger.warn(t.getMessage); jvalue})) // To Either[Json, Message]
         .map(msg => msg match {
            case Left(value: JsValue) => {logger.warn(s"The following MQTT message cannot be converted to Message: ${value}");  None }
            case Right(m: Message) => Some(m)
         })
         .filter(_.isDefined).map(_.get)
         .map(msg => new ProducerRecord[String, String](kafkaTopic, msg.metric, msg.toJson.compactPrint))
         .to(Producer.plainSink(producerSettings))

      val (bound: Future[Tcp.ServerBinding], server: UniqueKillSwitch) = Try {
        graph.run()
      } match {
        case Failure(_:KafkaException) =>
          Console.err.println(s"Cannot connect to the Kafka host: $kafkaHost:$kafkaPort.")
          sys.exit(1)
        case Failure(e) =>
          Console.err.println(s"Unexpected error has occurred when tried to connect to Kafka host:\n$e")
          sys.exit(2)
        case Success(value) => value
      }

      scala.sys.addShutdownHook {
         println("Shutdown...")

         bound
            .flatMap(_.unbind()) // trigger unbinding from the port
            .onComplete(_ => {
               system.terminate()
            }) // and shutdown when done
      }
   }
}
