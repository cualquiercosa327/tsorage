package be.cetic.tsorage.hub.filter

import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class AndTest extends FlatSpec with Matchers with FilterJsonProtocol {
   private val f1 = TagExist("my_tag")
   private val f2 = TagFilter("my_key", "my_value")
   private val filter = And(TagExist("my_tag"), TagFilter("my_key", "my_value"))
   private val message = """ ["and", ["+", "my_tag"], ["=", "my_key", "my_value"]] """.parseJson

   "A AND filter" should "be retrieved when an appropriate json array is parsed" in {
      message.convertTo[Filter] shouldEqual filter
   }

   it should "Be converted to an appropriate json object" in {
      filter.asInstanceOf[Filter].toJson shouldEqual message
   }
}
