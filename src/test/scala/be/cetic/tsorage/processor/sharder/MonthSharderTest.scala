package be.cetic.tsorage.processor.sharder

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

class MonthSharderTest extends FlatSpec with Matchers
{
   "A Month sharder" should "provide an appropriated shard" in {
      val dates = Map(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000) -> "2019-08",
         LocalDateTime.of(2019, 8, 1, 0, 0, 0, 0) -> "2019-08",
      )

      dates.foreach(d => MonthSharder.shard(d._1) shouldBe d._2)
   }
}
