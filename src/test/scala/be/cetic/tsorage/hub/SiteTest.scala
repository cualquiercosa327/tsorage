package be.cetic.tsorage.hub

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.scalatest.{Matchers, WordSpec}

import scala.io.Source

class SiteTest extends WordSpec with Matchers with ScalatestRouteTest {
  val testConnectionRoute: Route = Site.testConnectionRoute
  val swaggerRoute: Route = Site.swaggerRoute

  "The service" should {
    // Test connection route.
    "return OK for GET requests to the root path" in {
      Get() ~> testConnectionRoute ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    // Swagger route (API's documentation).
    "return the HTML documentation for GET requests to the Swagger path" in {
      Get("/swagger") ~> swaggerRoute ~> check {
        val docFile = Source.fromFile("src/main/resources/swagger-ui/index.html")
        val documentation = docFile.mkString
        docFile.close()

        responseAs[String] shouldEqual documentation
      }
    }
  }
}
