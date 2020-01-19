package be.cetic.tsorage.processor.flow

import java.time.{LocalDateTime, ZoneId}

import akka.NotUsed
import akka.stream.{FanOutShape, FanOutShape3, FlowShape, Inlet, Outlet, OverflowStrategy, Shape, SinkShape, UniformFanOutShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import be.cetic.tsorage.common.messaging.{AggUpdate, Message, Observation}
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import com.typesafe.scalalogging.LazyLogging
import GraphDSL.Implicits._
import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.processor.aggregator.followup.AggAggregator
import be.cetic.tsorage.processor.aggregator.raw.{RawAggregator, SimpleRawDerivator}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.{ShardedTagsetUpdate, TimeAggregatorRawUpdate}
import spray.json.JsValue

import scala.collection.immutable
import scala.concurrent.{ExecutionContextExecutor, Future}



/**
 * A data block for processing incoming messages.
 *
 *
 *
 *            + ------------------ +      + - +    + --------------- +
 * Message -> | Message validation | ---> |   | -> | to TagsetUpdate | --------------------------------------> O
 *            + ------------------ +      |   |    + --------------- +
 *                      |                 |   |
 *                      v                 |   |    + --------------- +    + - +
 *            + ------------------ +      |   | -> |   Write Msg     | -> |   |    + ----- +    + ---------- +    + ----- +
 *            |   Unsupported Msg  |      |   |    + --------------- +    |   | -> | to RU | -> | to records | -> | to AU | -> O
 *            + ------------------ +      + - +                           |   |    + ----- +    + ---------- +    + ----- +
 *                                                                        |   |
 *                                                                        |   |
 *                                                                        |   | ----------------------------> O
 *                                                                        + - +
 *
 */
object MessageBlock extends LazyLogging
{
   /**
    * Creates a flow converting messages to raw updates. A raw update is the marker of a first aggregation that
    * should be updated. The raw update relies on the first temporal aggregation.
    *
    * If there is no first temporal aggregation, the created flow is a dead end, that produces nothing.
    *
    * @param firstAggregator  The first temporal aggregation.
    * @param builder
    * @return
    */
   private def msgToRU(firstAggregator: Option[TimeAggregator])(implicit builder: GraphDSL.Builder[NotUsed]):
   FlowShape[Message, TimeAggregatorRawUpdate] = firstAggregator match
   {
      case None => Utils.deadEnd

      case Some(aggregator) => {
         builder.add(
            Flow[Message]
               .mapConcat(msg => {
                  val dates = msg.values.map(value => aggregator.shunk(value._1)).distinct
                  dates.map(dt => TimeAggregatorRawUpdate(TimeSeries(msg.metric, msg.tagset), dt, msg.`type`, aggregator))
               })
         )
      }
   }

   /**
    * @param firstAggregator  The first temporal aggregator, if any.
    * @param derivators       All the derivators transforming raw values into aggregated values, in order to
    *                         perform the first time aggregation
    * @param context
    * @return
    */
   def createGraph(
                     firstAggregator: Option[TimeAggregator],
                     derivators: List[RawAggregator]
                  )(implicit context: ExecutionContextExecutor) = GraphDSL.create()
   { implicit builder: GraphDSL.Builder[NotUsed] =>

      lazy val sharder = Cassandra.sharder

      val messageValidation = builder.add(
         ConditionalRouter({msg: Message => DataTypeSupport.availableSupports.keySet contains msg.`type`})
      )

      val invalidSink = builder.add(
         Flow[Message].to(Sink.foreach(msg => logger.warn(s"Invalid message has been discarded: ${msg}")))
      )

      val toTU = builder.add(
         Flow[Message].mapConcat(msg =>
            msg.values.map(value => sharder.shard(value._1).distinct).map(shard => ShardedTagsetUpdate(msg.metric, msg.tagset, shard)))
      )


      val ru2AU = Utils.ru2Agg(derivators, firstAggregator)

      val broadcast1 = builder.add(Broadcast[Message](2))
      val broadcast2 = builder.add(Broadcast[Message](2))

      val writeMsg: FlowShape[Message, Message] = builder.add(CassandraWriter.createWriteMsgFlow(context))

      messageValidation.matching ~> broadcast1 ~> toTU
                                    broadcast1 ~> writeMsg ~> broadcast2 ~> msgToRU(firstAggregator) ~> ru2AU
      messageValidation.unmatching ~> invalidSink

      MessageBlock(
         messageValidation.in,
         toTU.out,
         ru2AU.out,
         broadcast2.out(1)
      )
   }.named("Message Processing")
}

case class MessageBlock(
                       message: Inlet[Message],
                       tu: Outlet[ShardedTagsetUpdate],
                       au: Outlet[AggUpdate],
                       processedMessage: Outlet[Message]
                       ) extends Shape
{
   override def inlets: immutable.Seq[Inlet[_]] = message :: Nil

   override def outlets: immutable.Seq[Outlet[_]] = tu :: au :: processedMessage :: Nil

   override def deepCopy(): Shape = MessageBlock(
      message.carbonCopy(),
      tu.carbonCopy(),
      au.carbonCopy(),
      processedMessage.carbonCopy()
   )
}