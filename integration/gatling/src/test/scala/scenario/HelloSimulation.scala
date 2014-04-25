package scenario

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.AtOnceInjection

class HelloSimulation extends Simulation {
  val scn = scenario("Get hello world").exec(http("Hello world page").get("http://localhost:9000/hello.php"))

  setUp(scn.inject(AtOnceInjection(10)))
}
