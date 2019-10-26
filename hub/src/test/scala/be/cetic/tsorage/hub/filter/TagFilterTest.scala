package be.cetic.tsorage.hub.filter

import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class TagFilterTest extends FlatSpec with Matchers with FilterJsonProtocol {
   "A tag filter" should "be retrieved when an appropriate json array is parsed" in {
      val message = """ ["=", "key", "value"] """.parseJson

      message.convertTo[Filter] shouldEqual TagFilter("key", "value")
   }

   it should "Be converted to an appropriate json object" in {
      TagFilter("key", "value").asInstanceOf[Filter].toJson shouldEqual """ ["=", "key", "value"] """.parseJson
   }
}
