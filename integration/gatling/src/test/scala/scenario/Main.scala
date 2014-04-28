/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package scenario

import io.gatling.core.runner.{Runner, Selection}
import io.gatling.core.scenario.Simulation
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.reader.DataReader
import io.gatling.charts.report.ReportsGenerator

object Main extends App {
//  val selection = Selection(classOf[HelloSimulation].asInstanceOf[Class[Simulation]], "HelloSimulation", "Hello world")
  val selection = Selection(classOf[GenDataSimulation].asInstanceOf[Class[Simulation]], "GenDataSimulation", "Generate data")

  GatlingConfiguration.setUp()

  val (id, simulation) = new Runner(selection).run

  val dataReader = DataReader.newInstance(id)

  ReportsGenerator.generateFor("reports", dataReader)
}
