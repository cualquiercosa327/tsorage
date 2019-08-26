package be.cetic.tsorage.processor.flow

import akka.stream.ActorMaterializer
import be.cetic.tsorage.processor.sharder.MonthSharder
import be.cetic.tsorage.processor.source.RandomMessageIterator

import scala.concurrent.ExecutionContextExecutor

class TestFlow()(implicit val mat: ActorMaterializer, ec: ExecutionContextExecutor) {

  val messages = RandomMessageIterator.source()
  val sharder = MonthSharder

  val cassandraFlow = new CassandraFlow(sharder)
  val observationFlow = new ObservationFlow()

  val flow = messages
    .map(cassandraFlow.notifyTagnames)
    .mapConcat(observationFlow.fanOutObservations)
    .via(cassandraFlow.rawFlow)

}
