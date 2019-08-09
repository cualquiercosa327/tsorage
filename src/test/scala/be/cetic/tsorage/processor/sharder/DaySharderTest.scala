package be.cetic.tsorage.processor.sharder

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

class DaySharderTest extends FlatSpec with Matchers
{
   "A Day sharder" should "provide an appropriated shard" in {
      val dates = Map(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000) -> "2019-08-09",
         LocalDateTime.of(2019, 8, 1, 0, 0, 0, 0) -> "2019-08-01",
      )

      dates.foreach(d => DaySharder.shard(d._1) shouldBe d._2)
   }
}
