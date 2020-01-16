package be.cetic.tsorage.processor.flow

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.{FlowShape, Inlet, Outlet, OverflowStrategy, Shape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, Sink, Source}
import be.cetic.tsorage.common.messaging.{AggUpdate, Observation}
import com.typesafe.scalalogging.LazyLogging
import GraphDSL.Implicits._
import be.cetic.tsorage.common.json.AggUpdateJsonSupport
import be.cetic.tsorage.processor.WireTape
import be.cetic.tsorage.processor.aggregator.followup.AggAggregator
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.ShardedTagsetUpdate
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json._

import scala.collection.immutable

/**
 * Wrapper class representing a trigger for followup aggregation.
 * @param au   An aggregated update that has been performed.
 * @param ta   A time aggregator that must be performed.
 * @param derivator  The aggregation to perform.
 */
private case class FollowUpAggregationTrigger(au: AggUpdate, ta: TimeAggregator, derivator: AggAggregator)
                                     (implicit ec: ExecutionContextExecutor)
{
   def aggregations: Future[List[AggUpdate]] = derivator.aggregate(au, ta)
}

/**
  * A factory for creating processing graph handling the aggregations following the first one.
  * For the factory managing the graph responsible of the first aggregation, see FirstAggregationProcessingGraphFactory.
  *
  * Input: AggUpdate, informing an aggregated value has been created/updated in the system.
  * Output: processedAU, AU that have been stored
  *         Tag Updates, to be recorded
  *
  *             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  *             |                                                   |
  *             |  + - +    + -------- +    + - +    + --------- +  |
  *         (p) |~>|   | ~> | Write AU | ~> |   | ~> | Derive AU | ~|
  *                |   |    + -------- +    | 2 |    + --------- +
 *                 |   |                    |   |
 *                 |   |                    |   | ----------> O (processedAU)
 *                 |   |                    + - +
 *                 |   |
  * AggUpdate ~~~~>| 1 |    + -------- +
  *                |   | ~> | Kakfa AU | ~~~~~~~~~~~~~~~~~~~> O (Kafka)
 *                 |   |    + -------- +
 *                 |   |    + ---------------------- +
 *                 |   | ~> | to SharedTagsetUpdates | ~~~~~> O (tu)
 *                 |   |    + ---------------------- +
 *                 + - +
  */
object AggregationBlock extends LazyLogging with AggUpdateJsonSupport
{
   private lazy val sharder = Cassandra.sharder

   /**
    * Converts an aggregation into follow-up aggregations.
    */
   private def aggToAgg(derivators: List[AggAggregator], timeAggregators: List[TimeAggregator])
                       (implicit builder: GraphDSL.Builder[NotUsed], ec: ExecutionContextExecutor) =
   {
      builder.add(
            Flow[AggUpdate].mapConcat(au => {
            timeAggregators
               .filter(ta => ta.previousName == au.interval)
               .map(ta => (au, ta))
            }).mapConcat(element => derivators.map(derivator => FollowUpAggregationTrigger(element._1, element._2, derivator)))
              .mapAsyncUnordered(4)(trigger => trigger.aggregations)
              .mapConcat(x => x)
            .buffer(5000, OverflowStrategy.fail)
      )
   }

   /**
    * Converts an aggregation update into sharded tagset updates.
    */
   private def aggToTU(implicit builder: GraphDSL.Builder[NotUsed]): FlowShape[AggUpdate, ShardedTagsetUpdate] =
      builder.add(Flow[AggUpdate].map(au => ShardedTagsetUpdate(au.metric, au.tagset, sharder.shard(au.datetime))))

   /**
     * @param followUpDerivators All the ways to derivate aggregations from an aggregation.
     * @param timeAggregators The followup the aggregators. The first time aggregator (used for converting raw values
    *                        into aggregated values) can be omitted, but its presence will simply be ignored.)
     * @param aggObsDerivators All the ways to derivate observations from an aggregation.
     */
   def createGraph(
                     followUpDerivators: List[AggAggregator],
                     timeAggregators: List[TimeAggregator],
                     aggObsDerivators: List[Flow[AggUpdate, Observation, _]]
                  )(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

      val merge = builder.add(MergePreferred[AggUpdate](1).async)
      val broadcast = builder.add(Broadcast[AggUpdate](3).async)

      val writeAgg = builder.add(CassandraWriter.createWriteAggFlow(context))

      val kafka = builder.add(
         Flow[AggUpdate]
            .map(au => new ProducerRecord[String, String]("aggregations", au.toJson.compactPrint))
      )

      val bc2 = builder.add(Broadcast[AggUpdate](2).async)
      val aggregations = aggToAgg(followUpDerivators, timeAggregators)

      val tu = aggToTU

      merge ~> broadcast

      broadcast ~> writeAgg ~> bc2
      bc2.out(0) ~> aggregations ~> merge.preferred
      broadcast ~> kafka
      broadcast ~> tu

      AggregationBlock(
         merge.in(0),
         kafka.out,
         tu.out,
         bc2.out(1),
      )

   }.named(s"Agg Processing")
}

case class AggregationBlock(
                              au: Inlet[AggUpdate],
                              kafka: Outlet[ProducerRecord[String, String]],
                              tu: Outlet[ShardedTagsetUpdate],
                              processedAU: Outlet[AggUpdate]
                           ) extends Shape
{
   override def inlets: immutable.Seq[Inlet[_]] = au :: Nil

   override def outlets: immutable.Seq[Outlet[_]] = kafka :: tu :: processedAU :: Nil

   override def deepCopy(): Shape = AggregationBlock(
      au.carbonCopy(),
      kafka.carbonCopy(),
      tu.carbonCopy(),
      processedAU.carbonCopy()
   )
}
