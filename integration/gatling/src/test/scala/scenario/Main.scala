package scenario

import io.gatling.core.runner.{Runner, Selection}
import io.gatling.core.scenario.Simulation
import io.gatling.core.config.GatlingConfiguration

object Main extends App {
  val selection = Selection(classOf[HelloSimulation].asInstanceOf[Class[Simulation]], "HelloSimulation", "Hello world")

  GatlingConfiguration.setUp()

  new Runner(selection).run
}
