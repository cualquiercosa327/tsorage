package be.cetic.tsorage.processor.flow

import com.datastax.driver.core.{ResultSet, ResultSetFuture}

import scala.concurrent.{ExecutionContextExecutor, Future}


trait FutureManager
{
   implicit def resultSetFutureToFutureResultSet(rsf: ResultSetFuture)(implicit ec: ExecutionContextExecutor): Future[ResultSet]=Future(rsf.get())
}
