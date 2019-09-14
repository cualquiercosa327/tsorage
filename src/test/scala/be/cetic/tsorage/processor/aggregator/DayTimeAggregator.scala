package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}


class DayTimeAggregator extends FlatSpec with Matchers
{
   val dt1 = LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000)
   val borderDT = LocalDateTime.of(2019, 8, 9, 0, 0, 0)

   "A Day aggregator" should "round to the next day" in {
      new DayAggregator("").shunk(dt1) shouldEqual LocalDateTime.of(2019, 8, 10, 0, 0, 0)
   }

   it should "retrieve the right border shunk" in {
      new DayAggregator("").shunk(borderDT) shouldEqual borderDT
   }

   it should "correctly detect a border" in {
      new DayAggregator("").isBorder(dt1) shouldBe false
      new DayAggregator("").isBorder(borderDT) shouldBe true
   }
}
