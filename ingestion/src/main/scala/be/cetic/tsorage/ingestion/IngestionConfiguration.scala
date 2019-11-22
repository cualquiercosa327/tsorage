package be.cetic.tsorage.ingestion

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

/**
 * Configuration of the ingestion module.
 *
 * Path of configuration file is tsorage/ingestion/src/main/resources/ingest-http.conf.
 *
 * To configure Kafka host, you have to set the TSORAGE_KAFKA_HOST environment variable (defaults to localhost). To
 * configure hub module host, you have to set the TSORAGE_HUB_HOST environment variable (defaults to localhost).
 *
 */
object IngestionConfiguration {
  private val kafkaHost = System.getenv().getOrDefault("TSORAGE_KAFKA_HOST", "localhost")
  private val hubHost = System.getenv().getOrDefault("TSORAGE_HUB_HOST", "http://localhost")

  val conf: Config = ConfigFactory.load("ingest-http.conf")
    .withValue("kafka.host", ConfigValueFactory.fromAnyRef(s"$kafkaHost"))
    .withValue("authentication.host", ConfigValueFactory.fromAnyRef(s"$hubHost"))
}
