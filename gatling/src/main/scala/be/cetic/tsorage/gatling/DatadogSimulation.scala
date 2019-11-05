package be.cetic.tsorage.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class DatadogSimulation extends Simulation
{
   val httpConf = http
      .baseUrl("http://localhost:8080")
      .acceptHeader("application/json")
      .doNotTrackHeader("1")
      .acceptLanguageHeader("en-US,en;q=0.5")

   val scn = scenario("BasicSimulation")
      .exec(http("")
         .post("api/v1/series"))
      .pause(5)

   setUp(
      scn.inject(atOnceUsers(1))
   ).protocols(httpConf)

}
