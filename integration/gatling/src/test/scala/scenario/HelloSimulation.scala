/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

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
