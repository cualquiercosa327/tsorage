package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

class DatedSupportTest() extends FlatSpec with Matchers
{
   "A dated double" should "be correctly converted into UDT" in {
      val test = DatedType[Double](LocalDateTime.now, 42)
      val support = DatedTypeSupport(DoubleSupport)

      val udt = support.asAggUdtValue(test)
   }

   "An UDT representation of an dated double" should "be correctly converted back to Dated double" in {
      val test = DatedType[Double](LocalDateTime.now, 42)
      val support = DatedTypeSupport(DoubleSupport)
      val udt = support.asAggUdtValue(test)
      val res = support.fromUDTValue(udt)
      val payload = res.value

      println(payload)
   }
}
