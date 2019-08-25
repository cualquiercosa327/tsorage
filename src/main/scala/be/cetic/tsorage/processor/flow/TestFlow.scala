package be.cetic.tsorage.processor.flow

import java.util.concurrent.TimeUnit

import akka.stream.ActorMaterializer
import be.cetic.tsorage.processor.aggregator.{MinuteAggregator, TimeAggregator}
import be.cetic.tsorage.processor.sharder.{MonthSharder, Sharder}
import be.cetic.tsorage.processor.source.RandomMessageIterator
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration

class TestFlow(val aggregators: List[TimeAggregator], val sharder: Sharder)(implicit val mat: ActorMaterializer) {
  val messages = RandomMessageIterator.source()

  val cassandraFlow = new CassandraFlow(sharder)
  val observationFlow = new ObservationFlow()

  val conf = ConfigFactory.load("storage.conf")

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
       // TODO : Group if you whish optimize it
       .map(c => agg.updateShunk(c._1, c._2, c._3))
  })

}
