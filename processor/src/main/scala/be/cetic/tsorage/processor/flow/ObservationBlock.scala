package be.cetic.tsorage.processor.flow

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred}
import be.cetic.tsorage.common.messaging.{AggUpdate, Message, Observation}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.update.{ShardedTagsetUpdate, TimeAggregatorRawUpdate}
import spray.json.JsValue
import GraphDSL.Implicits._
import akka.stream.{FlowShape, Inlet, Outlet, Shape}
import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.processor.aggregator.raw.{RawAggregator, SimpleRawAggregator}
import be.cetic.tsorage.processor.database.Cassandra
import org.apache.kafka.clients.producer.ProducerRecord

import scala.collection.immutable
import scala.concurrent.ExecutionContextExecutor

/**
 * A logic block dedicated to observation processing.
 * It takes observations from the Message and the Aggregation blocks, and process them.
 * As an output, this block produces tags to be updated, as well as aggregated updates.
 *
 * First-level aggregated updates are also generated based on derivated observations.
 *
 * This block takes into account two kinds of observation:
 *
 *    - Processed Messages, that have already processed observations. These observations can be derivated.
 *    - Processed AggUpdate. While the aggupdate has already been processed, derivated observation can be generated
 *      in this block.
 *
 *
 *
 * (processed)  + ------ +    + - +     + ------------ +                               + - +
 * Message  --> | to Obs | -> |   |  -> | as Kafka Msg | ----------------------------> |   |
 *              + ------ +    |   |     + ------------ +                               |   |
 *                            | 1 |                                                    | 3 | ---------------> O (Kafka[Obs])
 *                            |   |     + ---------- +    + - +    + ------------ +    |   |
 *                            |   |  -> | Derive Obs | -> |   | -> | as Kafka Msg | -> |   |
 *                            + - +     + ---------- +    |   |    + ------------ +    + - +
 *                                                        |   |
 *                                                        |   |    + --------- +    + --------- +
 *              + ---------- +                            |   | -> | Write Obs | -> | First Agg | ----------> O (AggUpdate)
 * AggUpdate -> | Derive Obs | -------------------------> |   |    + --------- +    + --------- +
 *              + ---------- +                            | 2 |
 *                                                        |   |    + --------------- +
 *                                                        |   | -> | to S Tag Update | ---------------------> O (S Tag Update)
 *                                                        |   |    + --------------- +
 *                                                        |   |
 *                                                    (p) |   |    + ---------- +
 *                                                   + -> |   | -> | Derive Obs | - +
 *                                                   |    + - +    + ---------- +   |
 *                                                   + ---------------------------- +
 *
 */
object ObservationBlock
{
   /**
    * Derive observations from observations.
    * @return
    */
   private def obsToObs(derivators: List[Flow[Observation, Observation, _]])(implicit builder: GraphDSL.Builder[NotUsed]) =
   {
      derivators match {
         case Nil => Utils.deadEnd
         case _ => {
            val broadcast = builder.add(Broadcast[Observation](derivators.size).async)
            val merge = builder.add(Merge[Observation](derivators.size))

            derivators.foreach(derivator => {
               val d = builder.add(derivator)
               broadcast ~> d ~> merge
            })

            FlowShape[Observation, Observation](broadcast.in, merge.out)
         }
      }
   }

   def createGraph(
                     firstAggregator: Option[TimeAggregator],
                     derivators: List[RawAggregator],
                     autoObs: List[AggUpdate => List[Observation]]
                  )
                  (implicit context: ExecutionContextExecutor) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

      val msg2Obs = builder.add(
         Flow[Message].mapConcat(msg => msg.values.map(value => Observation(msg.metric, msg.tagset, value._1, value._2, msg.`type`)))
      )

      val writeObs: FlowShape[Observation, Observation] = builder.add(CassandraWriter.createWriteObsFlow(context))

      val ruToAU = Utils.ru2Agg(derivators, firstAggregator)

      val obsToTU = builder.add(
         Flow[Observation].map(obs => ShardedTagsetUpdate(obs.metric, obs.tagset, Cassandra.sharder.shard(obs.datetime)))
      )

      val obsToRu = firstAggregator match {
         case None => Utils.deadEnd

         case Some(agg) => builder.add(
            Flow[Observation].map(obs => TimeAggregatorRawUpdate(TimeSeries(obs.metric, obs.tagset), agg.shunk(obs.datetime), obs.`type`, agg))
         )
      }

       val au2Obs = builder.add(
          Flow[AggUpdate].mapConcat(au => {
             autoObs.flatMap(f => f(au))
         })
       )

      val bc1 = builder.add(Broadcast[Observation](2).async)
      val bc2 = builder.add(Broadcast[Observation](4).async)
      val merge2 = builder.add(MergePreferred[Observation](2))

      val merge3 = builder.add(Merge[ProducerRecord[String, String]](2))


      msg2Obs ~> bc1 ~> Utils.obsToKafkaMsg ~>                               merge3
                 bc1 ~> obsToObs(List()) ~> merge2.in(0)

                                     merge2 ~> bc2 ~> Utils.obsToKafkaMsg ~> merge3
                                               bc2 ~> writeObs ~> obsToRu ~> ruToAU
                                               bc2 ~> obsToTU
                                               bc2 ~> obsToObs(List()) ~> merge2.preferred

      au2Obs  ~>                     merge2.in(1)

      ObservationBlock(
         msg2Obs.in,
         au2Obs.in,
         merge3.out,
         ruToAU.out,
         obsToTU.out
      )
   }.named("Observation Processing")
}

case class ObservationBlock(
                              messageIn: Inlet[Message],
                              auIn: Inlet[AggUpdate],
                              kafka: Outlet[ProducerRecord[String, String]],
                              au: Outlet[AggUpdate],
                              tu: Outlet[ShardedTagsetUpdate]
                           ) extends Shape
{
   override def inlets: immutable.Seq[Inlet[_]] = messageIn :: auIn :: Nil

   override def outlets: immutable.Seq[Outlet[_]] = kafka :: au :: tu :: Nil

   override def deepCopy(): Shape = ObservationBlock(
      messageIn.carbonCopy(),
      auIn.carbonCopy(),
      kafka.carbonCopy(),
      au.carbonCopy(),
      tu.carbonCopy()
   )
}
