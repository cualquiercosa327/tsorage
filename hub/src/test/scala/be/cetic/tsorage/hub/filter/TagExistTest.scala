package be.cetic.tsorage.hub.filter

import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class TagExistTest extends FlatSpec with Matchers with FilterJsonProtocol {
   "A tagexist filter" should "be retrieved when an appropriate json array is parsed" in {
      val message = """ ["+", "key"] """.parseJson

      message.convertTo[Filter] shouldEqual TagExist("key")
   }

   it should "Be converted to an appropriate json object" in {
      TagExist("key").asInstanceOf[Filter].toJson shouldEqual """ ["+", "key"] """.parseJson
   }
}
