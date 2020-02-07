package be.cetic.tsorage.common

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}

object WireTape
{
   def createWireTape[I, O](flow: Flow[I, O, _]) =
      Flow[I].map(entry => {println("TAPE : " + entry); entry})
      .via(flow)

   def tape[T](indicator: String)(implicit builder: GraphDSL.Builder[NotUsed]): FlowShape[T, T] =
   {
      builder.add(
         Flow[T].map(element => {
            println(s"TAPE ${indicator}: ${element}")
            element
         })
      )
   }
}
