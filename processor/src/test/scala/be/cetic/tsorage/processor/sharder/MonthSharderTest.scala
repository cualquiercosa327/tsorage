package be.cetic.tsorage.processor.sharder

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

class MonthSharderTest extends FlatSpec with Matchers
{
   "A Month sharder" should "provide an appropriated shard" in {
      val dates = Map(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56, 789000) -> "2019-08",
         LocalDateTime.of(2019, 8, 1, 0, 0, 0, 0) -> "2019-08"
      )

      dates.foreach(d => MonthSharder.shard(d._1) shouldBe d._2)
   }

   it should "provide a unique shard for a time range included in a single month" in {
      MonthSharder.shards(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56),
         LocalDateTime.of(2019, 8, 13, 17, 24, 13)
      ) shouldBe List("2019-08")
   }

   it should "provide the right shard when the time range reaches a border by the left" in {
      MonthSharder.shards(
         LocalDateTime.of(2019, 8, 1, 0, 0, 0),
         LocalDateTime.of(2019, 8, 9, 17, 24, 13)
      ) shouldBe List("2019-08")
   }

   it should "provide the right shards when the time range reaches a border by the right" in {
      MonthSharder.shards(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56),
         LocalDateTime.of(2019, 9, 1, 0, 0, 0)
      ) shouldBe List("2019-08", "2019-09")
   }

   it should "provide the right shards when the time range crosses multiple shard" in {
      MonthSharder.shards(
         LocalDateTime.of(2019, 8, 9, 12, 34, 56),
         LocalDateTime.of(2019, 11, 13, 1, 0, 0)
      ) shouldBe List(
         "2019-08",
         "2019-09",
         "2019-10",
         "2019-11"
      )
   }
}
