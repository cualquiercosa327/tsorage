package be.cetic.tsorage.common

import com.datastax.driver.core.exceptions.NoHostAvailableException
import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}

object CassandraFactory {
  /**
   * Create a Cassandra session given a configuration.
   * If the Cassandra host is unavailable (cannot connect to it), then the program is exited with this exit code: 1.
   * If an unexpected error has occured, then the program is exited with this exit code: 2.
   *
   * @param conf A Configuration.
   * @return A Cassandra session.
   */
  def createCassandraSession(conf: Config): Session = {
    val cassandraHost = conf.getString("cassandra.host")
    val cassandraPort = conf.getInt("cassandra.port")

    val sessionTry = Try {
      Cluster.builder
        .addContactPoint(cassandraHost)
        .withPort(cassandraPort)
        .withoutJMXReporting()
        .build
        .connect()
    }

    sessionTry match {
      case Failure(_:NoHostAvailableException) =>
        Console.err.println(s"Cannot connect to the Cassandra host: $cassandraHost:$cassandraPort.")
        sys.exit(1)
      case Failure(e) =>
        Console.err.println(s"Unexpected error has occurred when tried to connect to Cassandra host:\n$e")
        sys.exit(2)
      case _ =>
    }

    sessionTry match {
      case Success(value) => value
    }
  }
}
