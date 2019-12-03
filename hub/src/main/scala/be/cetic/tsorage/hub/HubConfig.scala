package be.cetic.tsorage.hub

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.LazyLogging

/**
 * A representation of the Hub configuration.
 *
 * Path of configuration file is tsorage/hub/src/main/resources/hub.conf.
 *
 * To configure Cassandra host, you have to set the TSORAGE_CASSANDRA_HOST environment variable (defaults to localhost).
 *
 */
object HubConfig extends LazyLogging {
  private val cassandraHost = System.getenv().getOrDefault("TSORAGE_CASSANDRA_HOST", "localhost")

  val conf: Config = ConfigFactory.load("hub.conf")
    .withValue("cassandra.host", ConfigValueFactory.fromAnyRef(s"$cassandraHost"))
}
