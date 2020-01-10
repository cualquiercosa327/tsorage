package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import be.cetic.tsorage.common.FutureManager
import com.datastax.driver.core._

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * An alternative implementation to https://github.com/akka/alpakka/blob/v1.1.1/cassandra/src/main/scala/akka/stream/alpakka/cassandra/scaladsl/CassandraFlow.scala
  * that replace a static prepared statement by a function of the input,
  * which allow dynamic column insertions.
  */
object AltCassandraFlow extends FutureManager
{

   def createWithPassThrough[T](
                                  parallelism: Int,
                                  statement: T => PreparedStatement,
                                  statementBinder: (T, PreparedStatement) => BoundStatement
                               )(implicit session: Session, ec: ExecutionContextExecutor): Flow[T, T, NotUsed] =
      Flow[T].mapAsync(parallelism)(
         t ⇒{
            val fut: Future[ResultSet]= session.executeAsync(statementBinder(t, statement(t)))
              fut.map(_ => t)
         }
      ).named("Async Cassandra Sink")
}
