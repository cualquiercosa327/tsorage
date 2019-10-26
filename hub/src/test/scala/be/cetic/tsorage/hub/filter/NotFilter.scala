package be.cetic.tsorage.hub.filter

import org.scalatest.{FlatSpec, Matchers}
import spray.json._


class NotTest extends FlatSpec with Matchers with FilterJsonProtocol {
   private val f1 = TagExist("my_tag")
   private val filter = Not(TagExist("my_tag"))
   private val message = """ ["not", ["+", "my_tag"]] """.parseJson

   "A NOT filter" should "be retrieved when an appropriate json array is parsed" in {
      message.convertTo[Filter] shouldEqual filter
   }

   it should "be converted to an appropriate json object" in {
      filter.asInstanceOf[Filter].toJson shouldEqual message
   }
}
