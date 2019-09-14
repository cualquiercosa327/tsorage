package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}


class MonthTimeAggregator extends FlatSpec with Matchers
{
   val dt1 = LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000)
   val borderDT = LocalDateTime.of(2019, 8, 1, 0, 0, 0)


   "A Month aggregator" should "round to the next month" in {
      new MonthAggregator("").shunk(dt1) shouldEqual LocalDateTime.of(2019, 9, 1, 0, 0, 0)
   }

   "A Month aggregator" should "retrieve the right border shunk" in {
      new MonthAggregator("").shunk(borderDT) shouldEqual borderDT
   }
}
