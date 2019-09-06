package be.cetic.tsorage.processor.flow

import java.util.concurrent.TimeUnit

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import be.cetic.tsorage.processor.FloatMessage
import be.cetic.tsorage.processor.aggregator.{MinuteAggregator, TimeAggregator}
import be.cetic.tsorage.processor.sharder.{MonthSharder, Sharder}
import be.cetic.tsorage.processor.source.RandomMessageIterator
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContextExecutor

class TestFlow(val messages: Source[FloatMessage, _], val aggregators: List[TimeAggregator], val sharder: Sharder)(implicit val mat: ActorMaterializer, implicit val context: ExecutionContextExecutor) {

  val cassandraFlow = new CassandraFlow(sharder)
  val observationFlow = new ObservationFlow()

  val conf = ConfigFactory.load("tsorage.conf")

  val baseFlow = messages
       .map(cassandraFlow.notifyTagnames)
       .mapConcat(observationFlow.fanOutObservations)
       .via(cassandraFlow.rawFlow)
       .map(obs => (obs.metric, obs.tagset, obs.datetime))
       .groupedWithin(
         conf.getInt("grouper.size"),
         FiniteDuration(conf.getInt("grouper.duration"), conf.getString("grouper.duration_unit"))
       )
       .mapConcat(group => group.map(obs => (obs._1, obs._2, aggregators.head.shunk(obs._3))).toSet)

  val flow = aggregators.foldLeft(baseFlow)( (prev, agg) => {
    prev
       .map(change => (change._1, change._2, agg.shunk(change._3)))
       .map(c => agg.updateShunk(c._1, c._2, c._3))
  })

}
