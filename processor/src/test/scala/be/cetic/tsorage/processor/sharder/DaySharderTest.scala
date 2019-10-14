package be.cetic.tsorage.processor.sharder

import java.time.LocalDateTime

import be.cetic.tsorage.common.sharder.DaySharder
import org.scalatest.{FlatSpec, Matchers}

class DaySharderTest extends FlatSpec with Matchers
{
   "A Day sharder" should "provide an appropriated shard" in {
      val dates = Map(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000) -> "2019-08-09",
         LocalDateTime.of(2019, 8, 1, 0, 0, 0, 0) -> "2019-08-01"
      )

      dates.foreach(d => DaySharder.shard(d._1) shouldBe d._2)
   }

   it should "provide a unique shard for a time range included in a single day" in {
      DaySharder.shards(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56),
         LocalDateTime.of(2019, 8, 9, 17, 24, 13)
      ) shouldBe List("2019-08-09")
   }

   it should "provide the right shard when the time range reaches a border by the left" in {
      DaySharder.shards(
         LocalDateTime.of(2019, 8, 9, 0, 0, 0),
         LocalDateTime.of(2019, 8, 9, 17, 24, 13)
      ) shouldBe List("2019-08-09")
   }

   it should "provide the right shards when the time range reaches a border by the right" in {
      DaySharder.shards(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56),
         LocalDateTime.of(2019, 8, 10, 0, 0, 0)
      ) shouldBe List("2019-08-09", "2019-08-10")
   }

   it should "provide the right shards when the time range crosses multiple shard" in {
      DaySharder.shards(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56),
         LocalDateTime.of(2019, 8, 13, 1, 0, 0)
      ) shouldBe List(
         "2019-08-09",
         "2019-08-10",
         "2019-08-11",
         "2019-08-12",
         "2019-08-13"
      )
   }
}
