package scenario

import io.gatling.core.runner.{Runner, Selection}
import io.gatling.core.scenario.Simulation
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.reader.DataReader
import io.gatling.charts.report.ReportsGenerator

object Main extends App {
  val selection = Selection(classOf[HelloSimulation].asInstanceOf[Class[Simulation]], "HelloSimulation", "Hello world")

  GatlingConfiguration.setUp()

  val (id, simulation) = new Runner(selection).run

  val dataReader = DataReader.newInstance(id)

  ReportsGenerator.generateFor("reports", dataReader)
}
