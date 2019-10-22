package be.cetic.tsorage.hub

import be.cetic.tsorage.common.DateTimeConverter
import be.cetic.tsorage.common.sharder.Sharder
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.schemabuilder.SchemaBuilder.Direction
import com.datastax.driver.core.{Cluster, DataType, Session}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Cassandra database for unit tests.
 *
 * This database contains three metrics: "temperature", "pressure" and "humidity" whose the range of values of each
 * one is [-5, 33], [1005, 1040] and [0, 100] respectively (the values are generated randomly and uniformly). For
 * each metric, there are 1000 data between "2019-09-28T16:00:00.000Z" and "2019-10-05T14:30:00.000Z" in steps of 10
 * minutes.
 *
 */
class TestDatabase(private val conf: Config = ConfigFactory.load("test.conf")) extends LazyLogging {
  // TODO: move this class to src/test/scala/be/cetic/tsorage/hub/ for production.

  private val cassandraHost = conf.getString("cassandra.host")
  private val cassandraPort = conf.getInt("cassandra.port")

  private val keyspaceAgg = conf.getString("cassandra.keyspaces.other") // Keyspace containing aggregated data.
  private val keyspaceRaw = conf.getString("cassandra.keyspaces.raw") // Keyspace containing raw data.

  private val session: Session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .withoutJMXReporting()
    .build
    .connect()

  private val sharder = Sharder(conf.getString("sharder"))

  /**
   * Create the entire database. That is, keyspaces, tables and data.
   *
   */
  def create(): Unit = {
    // Create the keyspaces.
    createKeyspaces()

    // Create the user-defined types.
    createUdts()

    // Create the tables.
    createTables()

    // Generate and add data.
    generateAddData(1000, "2019-09-28T16:00:00.000Z", 10 * 60, Map(
      "temperature" -> (-5, 33),
      "pressure" -> (1005, 1040),
      "humidity" -> (0, 100)
    ))
  }

  /**
   * Create the keyspaces of this database.
   *
   */
  private def createKeyspaces(): Unit = {
    val replication = Map("class" -> "SimpleStrategy", "replication_factor" -> 1.toString.asInstanceOf[AnyRef]).asJava
    Seq(keyspaceAgg, keyspaceRaw).foreach(keyspace =>
      session.execute(
        SchemaBuilder.createKeyspace(keyspace)
          .`with`().replication(replication)
          .durableWrites(true)
      )
    )
  }

  /**
   * Create the user-defined types.
   *
   */
  private def createUdts(): Unit = {
    Seq(keyspaceAgg, keyspaceRaw).foreach { keyspace =>
      session.execute(
        SchemaBuilder.createType(keyspace, "tdouble")
          .addColumn("value", DataType.cdouble)
      )

      session.execute(
        SchemaBuilder.createType(keyspace, "tlong")
          .addColumn("value", DataType.bigint)
      )

      session.execute(
        SchemaBuilder.createType(keyspace, "date_double")
          .addColumn("datetime", DataType.timestamp())
          .addColumn("value", DataType.cdouble)
      )
    }
  }

  /**
   * Create the tables.
   *
   */
  private def createTables(): Unit = {
    session.execute(
      SchemaBuilder.createTable(keyspaceAgg, "observations")
        .addPartitionKey("metric_", DataType.text)
        .addPartitionKey("shard_", DataType.text)
        .addPartitionKey("interval_", DataType.text)
        .addPartitionKey("aggregator_", DataType.text)
        .addClusteringColumn("datetime_", DataType.timestamp)
        .addUDTColumn("value_double_", SchemaBuilder.udtLiteral("tdouble"))
        .addUDTColumn("value_long_", SchemaBuilder.udtLiteral("tlong"))
        .addUDTColumn("value_date_double_", SchemaBuilder.udtLiteral("date_double"))
        .withOptions().clusteringOrder("datetime_", Direction.DESC)
    )

    session.execute(
      SchemaBuilder.createTable(keyspaceRaw, "observations")
        .addPartitionKey("metric_", DataType.text)
        .addPartitionKey("shard_", DataType.text)
        .addClusteringColumn("datetime_", DataType.timestamp)
        .addUDTColumn("value_double_", SchemaBuilder.udtLiteral("tdouble"))
        .addUDTColumn("value_long_", SchemaBuilder.udtLiteral("tlong"))
        .addUDTColumn("value_date_double_", SchemaBuilder.udtLiteral("date_double"))
        .withOptions().clusteringOrder("datetime_", Direction.DESC)
    )

    session.execute(
      SchemaBuilder.createTable(keyspaceAgg, "static_tagset")
        .addPartitionKey("metric", DataType.text)
        .addClusteringColumn("tagname", DataType.text)
        .addColumn("tagvalue", DataType.text)
        .withOptions().clusteringOrder("tagname", Direction.ASC)
    )

    session.execute(
      SchemaBuilder.createTable(keyspaceAgg, "dynamic_tagset")
        .addPartitionKey("metric", DataType.text)
        .addClusteringColumn("shard", DataType.text())
        .addClusteringColumn("tagname", DataType.text())
        .addClusteringColumn("tagvalue", DataType.text())
        .withOptions().clusteringOrder("shard", Direction.DESC)
        .clusteringOrder("tagname", Direction.ASC)
        .clusteringOrder("tagvalue", Direction.ASC)
    )
  }

  /**
   * Generate and add random data.
   *
   * @param numData          the number of data to generate per each metric.
   * @param startTime        A start time in ISO 8601 format.
   * @param timeStepSec      the time step in seconds between two consecutive data.
   * @param metricValueRange a map where each key is a name of one metric and each value is a range of values for the
   *                         corresponding metric.
   * @param seed             a seed for the random number generator.
   */
  private def generateAddData(numData: Int, startTime: String, timeStepSec: Int,
                              metricValueRange: Map[String, (Int, Int)], seed: Int = 42): Unit = {
    assert(numData > 0, "The number of data must be positive.")
    assert(Try(DateTimeConverter.strToEpochMilli(startTime)).isSuccess, "The start time must be in ISO 8601 format.")
    assert(timeStepSec > 0, "The time step must be positive.")
    assert(
      metricValueRange.map { case (_, valueRange) =>
        valueRange._1 <= valueRange._2
      }.count(_ == true) == metricValueRange.size,
      "The range of values must be valid; the first component must be less than the second component."
    )

    // Extract some UDTs.
    val tDoubleType = session.getCluster.getMetadata.getKeyspace(keyspaceRaw).getUserType("tdouble")

    // Generate and add `numData` data for each metric.
    val random = new scala.util.Random(seed)
    val startTimestamp = DateTimeConverter.strToEpochMilli(startTime)
    metricValueRange.foreach { case (metric, valueRange) =>
      // Add some tagset in order to the database contains the name of `metric`.
      session.executeAsync(
        QueryBuilder.insertInto(keyspaceAgg, "static_tagset")
          .value("metric", metric)
          .value("tagname", "some_tag")
          .value("tagvalue", "some_value")
      )

      // Generate and add `numData` data for `metric``
      for (i <- Seq.range(0, numData)) {
        // Compute the timestamp and the corresponding shard for this data.
        val timestamp = startTimestamp + (i * timeStepSec * 1000)
        val shard = sharder.shard(DateTimeConverter.epochMilliToLocalDateTime(timestamp))

        // Generate a single value.
        var value = valueRange._1 + random.nextDouble() * (valueRange._2 - valueRange._1)
        value = value - (value % 0.01) // Round to two decimal points.
        val valueDouble = tDoubleType.newValue().setDouble("value", value)

        session.executeAsync(
          QueryBuilder.insertInto(keyspaceRaw, "observations")
            .value("metric_", metric)
            .value("shard_", shard)
            .value("datetime_", timestamp)
            .value("value_double_", valueDouble)
        )
      }
    }
  }

  /**
   * Clean the test database. All keyspaces (and therefore also tables) will be dropped.
   *
   */
  def clean(): Unit = {
    Seq(keyspaceAgg, keyspaceRaw).foreach(keyspace =>
      if (Option(session.getCluster.getMetadata.getKeyspace(keyspace)).isDefined) {
        // If `keyspace` exists.
        session.execute(
          SchemaBuilder.dropKeyspace(keyspace)
        )
      }
    )
  }
}
