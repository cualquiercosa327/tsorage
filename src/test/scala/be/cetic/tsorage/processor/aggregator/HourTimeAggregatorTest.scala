package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

class HourTimeAggregatorTest extends FlatSpec with Matchers
{
   val dt1 = LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000)
   val borderDT = LocalDateTime.of(2019, 8, 9, 12, 0, 0)

   "A Hour aggregator" should "round to the next hour" in {
      new HourAggregator("").shunk(dt1) shouldEqual LocalDateTime.of(2019, 8, 9, 13, 0, 0)
   }

   it should "retrieve the right border shunk" in {
      new HourAggregator("").shunk(borderDT) shouldEqual borderDT
   }

   it should "correctly detect a border" in {
      new HourAggregator("").isBorder(dt1) shouldBe false
      new HourAggregator("").isBorder(borderDT) shouldBe true
   }
}
