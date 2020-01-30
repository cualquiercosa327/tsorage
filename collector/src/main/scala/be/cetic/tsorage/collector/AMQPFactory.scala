package be.cetic.tsorage.collector

import akka.{Done, NotUsed}
import akka.stream.alpakka.amqp.{AmqpCachedConnectionProvider, AmqpUriConnectionProvider, AmqpWriteSettings, NamedQueueSourceSettings, QueueDeclaration}
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import akka.util.ByteString
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A sink for sending byte frames to AMQP.
 */
object AMQPFactory
{
   private def bufferURI(bufferConf: Config): String =
   {
      val bufferHost = bufferConf.getString("host")
      val bufferPort = bufferConf.getInt("port")
      val bufferUser = bufferConf.getString("user")
      val bufferPassword = bufferConf.getString("password")

      val amqpAuthority = s"${bufferUser}:${bufferPassword}@${bufferHost}:${bufferPort}"
      s"amqp://${amqpAuthority}"
   }

   def buildAMQPSink(bufferConf: Config)(implicit context: ExecutionContextExecutor) =
   {
      val connectionProvider = AmqpCachedConnectionProvider(AmqpUriConnectionProvider(bufferURI(bufferConf)))

      val queueName = bufferConf.getString("queue_name")
      val queueDeclaration = QueueDeclaration(queueName).withDurable(true)

      AmqpSink.simple(
         AmqpWriteSettings(connectionProvider)
            .withRoutingKey(queueName)
            .withDeclaration(queueDeclaration)
      )
   }

   def buildAMQPSource(bufferConf: Config)(implicit context: ExecutionContextExecutor) =
   {
      val connectionProvider = AmqpCachedConnectionProvider(AmqpUriConnectionProvider(bufferURI(bufferConf)))

      val queueName = bufferConf.getString("queue_name")
      val queueDeclaration = QueueDeclaration(queueName).withDurable(true)

      AmqpSource.committableSource(
         NamedQueueSourceSettings(connectionProvider, queueName).withDeclaration(queueDeclaration),
         bufferSize = 10
      )
   }
}
