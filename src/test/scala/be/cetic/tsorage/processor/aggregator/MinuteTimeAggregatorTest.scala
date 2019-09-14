package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}


class MinuteTimeAggregatorTest extends FlatSpec with Matchers
{
   val dt1 = LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000)
   val borderDT = LocalDateTime.of(2019, 8, 9, 12, 34, 0)


   "A Minute aggregator" should "round to the next minute" in {
      new MinuteAggregator("").shunk(dt1) shouldEqual LocalDateTime.of(2019, 8, 9, 12, 35, 0)
   }

   "A Minute aggregator" should "retrieve the right border shunk" in {
      new MinuteAggregator("").shunk(borderDT) shouldEqual borderDT
   }
}
