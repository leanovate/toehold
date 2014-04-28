package scenario

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.AtOnceInjection
import bootstrap._

class HelloSimulation extends Simulation {
  val scn = scenario("Get hello world").repeat(100) {
    exec(
      http("Hello world page")
        .get("http://localhost:9000/hello.php")
        .check(
          bodyString.is("Hello World")
        ))
  }

  setUp(scn.inject(AtOnceInjection(100)))
}
