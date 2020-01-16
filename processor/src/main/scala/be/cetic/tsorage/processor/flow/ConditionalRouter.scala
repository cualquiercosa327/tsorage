package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL}
import akka.stream.{FlowShape, Inlet, Outlet, Shape, UniformFanOutShape}
import GraphDSL.Implicits._
import be.cetic.tsorage.processor.WireTape

import scala.collection.immutable

/**
 * A message router based on a predicate.
 * The incoming messages matching the predicate are redirected to the first output ("matching");
 * the other are redirected to the second output ("unmatching").
 *
 * The predicate is evaluated only once for each incoming messages.
 *
 *             + ------ +
 *             |        | -> matching
 * Message ->  | Router |
 *             |        | -> unmatching
 *             + ------ +
 *
 */
object ConditionalRouter
{
   /**
    * @param predicate  The predicate to evaluate in order to determine if an element must
    *                   be sent to the matching port or the unmatching port.
    * @tparam T
    * @return
    */
   def apply[T](predicate: T => Boolean) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val wrapper = builder.add(Flow[T].map(msg => (msg, predicate(msg))))
         val broadcast = builder.add(Broadcast[(T, Boolean)](2))

         val unwrapperMatching: FlowShape[(T, Boolean), T] = builder.add(
            Flow[(T, Boolean)].filter(_._2).map(_._1)
         )

         val unwrapperUnMatching: FlowShape[(T, Boolean), T] = builder.add(
            Flow[(T, Boolean)].filterNot(_._2).map(_._1)
         )

         wrapper ~> broadcast

         broadcast ~> unwrapperMatching
         broadcast ~> unwrapperUnMatching

         new ConditionalRouter(wrapper.in, unwrapperMatching.out, unwrapperUnMatching.out)
   }
}

case class ConditionalRouter[T](in: Inlet[T], matching: Outlet[T], unmatching: Outlet[T]) extends Shape
{
   override def inlets: immutable.Seq[Inlet[_]] = in :: Nil

   override def outlets: immutable.Seq[Outlet[_]] = matching :: unmatching :: Nil

   override def deepCopy(): Shape = ConditionalRouter(
      in.carbonCopy(),
      matching.carbonCopy(),
      unmatching.carbonCopy()
   )
}
