package be.cetic.tsorage.processor.aggregator.data.position2d

import java.time.{LocalDateTime, ZoneId}
import java.util.Date
import org.scalatest.{FlatSpec, Matchers}

class MaximumLatitudeAggregationTest extends FlatSpec with Matchers
{
   private val l = List(
      (Date.from( LocalDateTime.now().atZone( ZoneId.systemDefault()).toInstant()), Position2D(1, 1)),
      (Date.from( LocalDateTime.now().atZone( ZoneId.systemDefault()).toInstant()), Position2D(-1, 2)),
      (Date.from( LocalDateTime.now().atZone( ZoneId.systemDefault()).toInstant()), Position2D(-2, -1)),
      (Date.from( LocalDateTime.now().atZone( ZoneId.systemDefault()).toInstant()), Position2D(2, -2))
   )

   "A list of Position2D" should "provide the element with the maximal latitude for raw aggregation" in {
      MaximumLatitudeAggregation.rawAggregation(l).value shouldBe Position2D(2, -2)
   }

   "it" should "provide the element with the maximal latitude for raw aggregation" in {
      MaximumLatitudeAggregation.aggAggregation(l).value shouldBe Position2D(2, -2)
   }
}