package be.cetic.tsorage.processor.database

import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.ConfigFactory

object Cassandra {
  private val conf = ConfigFactory.load("storage.conf")
  private val cassandraHost = conf.getString("cassandra.host")
  private val cassandraPort = conf.getInt("cassandra.port")

  val session: Session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .withoutJMXReporting()
    .build
    .connect()

  session
}
