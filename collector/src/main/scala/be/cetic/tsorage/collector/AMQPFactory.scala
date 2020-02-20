package be.cetic.tsorage.collector

import akka.{Done, NotUsed}
import akka.stream.alpakka.amqp.{AmqpCachedConnectionProvider, AmqpCredentials, AmqpDetailsConnectionProvider, AmqpUriConnectionProvider, AmqpWriteSettings, NamedQueueSourceSettings, QueueDeclaration}
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
   private def connectionProvider(bufferConf: Config) =
   {
      val host = bufferConf.getString("host")
      val port = bufferConf.getInt("port")

      val baseConnection = AmqpDetailsConnectionProvider(host, port)

      val advancedConnection = bufferConf.getString("security.type") match {
         case "anonymous" => baseConnection
         case "password" => {
            val user = bufferConf.getString("security.user")
            val password = bufferConf.getString("security.password")

            baseConnection.withCredentials(AmqpCredentials(user, password))
         }
      }

      AmqpCachedConnectionProvider(advancedConnection)
   }

   def buildAMQPSink(bufferConf: Config)(implicit context: ExecutionContextExecutor) =
   {
      val provider = connectionProvider(bufferConf)

      val queueName = bufferConf.getString("queue")
      val queueDeclaration = QueueDeclaration(queueName).withDurable(true)

      AmqpSink.simple(
         AmqpWriteSettings(provider)
            .withRoutingKey(queueName)
            .withDeclaration(queueDeclaration)
      )
   }

   def buildAMQPSource(bufferConf: Config)(implicit context: ExecutionContextExecutor) =
   {
      val provider = connectionProvider(bufferConf)

      val queueName = bufferConf.getString("queue")
      val queueDeclaration = QueueDeclaration(queueName).withDurable(true)

      AmqpSource.committableSource(
         NamedQueueSourceSettings(provider, queueName)
            .withDeclaration(queueDeclaration),
         bufferSize = 10
      )
   }
}
