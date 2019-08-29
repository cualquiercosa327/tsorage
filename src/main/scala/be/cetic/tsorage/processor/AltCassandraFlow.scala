package akka.stream.alpakka.cassandra.scaladsl
import akka.NotUsed
import akka.annotation.ApiMayChange
import akka.dispatch.ExecutionContexts
import akka.stream.FlowShape
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.scaladsl.{Flow, GraphDSL}
import com.datastax.driver.core.{BatchStatement, BoundStatement, PreparedStatement, Session}
import akka.stream.alpakka.cassandra.impl.GuavaFutures._

/**
  * An alternative implementation to https://github.com/akka/alpakka/blob/v1.1.1/cassandra/src/main/scala/akka/stream/alpakka/cassandra/scaladsl/CassandraFlow.scala
  * that replace a static prepared statement by a function of the input,
  * which allow dynamic column insertions.
  */
object AltCassandraFlow
{
   def createWithPassThrough[T](
                                  parallelism: Int,
                                  statement: T => PreparedStatement,
                                  statementBinder: (T, PreparedStatement) => BoundStatement
                               )(implicit session: Session): Flow[T, T, NotUsed] =
      Flow[T].mapAsync(parallelism)(
         t ⇒
            session
               .executeAsync(statementBinder(t, statement(t)))
               .asScala()
               .map(_ => t)(ExecutionContexts.sameThreadExecutionContext)
      )
}
