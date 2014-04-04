import sbt._
import sbt.Keys._
import play.PlayRunHook
import play.Project._
import scala.languageFeature.postfixOps
import sbtfilter.Plugin.Filter

object PhpFpmCommands {
  val phpFpmConfig = TaskKey[File]("php-fpm-config")

  val phpFpmProcess = AttributeKey[(Process, Thread)]("php-fpm-process")

  val settings = Seq(
    phpFpmConfig <<= (streams, baseDirectory) map {
      (s, base) =>
        val prefix = base / "target" / "php-fpm"
        val dest = prefix / "php-fpm.conf"
        val props = Seq(
          "baseDirectory" -> base.getAbsolutePath,
          "prefix" -> prefix.getAbsolutePath
        ).toMap
        IO.copyFile(base / "project" / "php-fpm.conf", dest, true)
        Filter(s.log, Seq(dest), props)
        dest
    },
    commands <++= baseDirectory {
      base => Seq(startPhpFpmCommand(base), stopPhpFpmProcess(base))
    },
    playRunHooks <+= (state, streams, baseDirectory, phpFpmConfig).map {
      (state, stream, base, config) => phpFpmRunHook(state, stream.log, base, config)
    }
  )

  def createPhpFpmProcess(log: Logger, base: File, config: File, args: String*) = {
    log.info(s"Running php-fpm --fpm-config ${config.getAbsolutePath} ${args.mkString(" ")}")
    Process("php-fpm" :: "--fpm-config" :: config.getAbsolutePath :: args.toList, base)
  }

  def startPhpFpmCommand(base: File): Command = Command.args("start-php-fpm", "<args>") {
    (state, args) =>
      state.get(phpFpmProcess).map {
        _ =>
          state.log.info("Alreaded started")
          state
      }.getOrElse {
        val project = Project.extract(state)
        val (nextState, config) = project.runTask(phpFpmConfig, state)
        val process = createPhpFpmProcess(state.log, base, config, args: _*).run()
        val terminator = new Thread {
          override def run() {
            process.destroy()
          }
        }
        java.lang.Runtime.getRuntime.addShutdownHook(terminator)

        nextState.put(phpFpmProcess, (process, terminator))
      }
  }

  def stopPhpFpmProcess(base: File): Command = Command.args("stop-php-fpm", "<>") {
    (state, args) =>
      state.get(phpFpmProcess).map {
        case (process, terminator) =>
          process.destroy()
          java.lang.Runtime.getRuntime.removeShutdownHook(terminator)
          state.remove(phpFpmProcess)
      }.getOrElse(state)
  }

  def phpFpmRunHook(state: State, log: Logger, base: File, config: File) = new PlayRunHook {

    var process: Option[Process] = None

    override def beforeStarted() = {
      state.get(phpFpmProcess).getOrElse {
        process = Some(createPhpFpmProcess(state.log, base, config).run())
      }
    }

    override def afterStopped(): Unit = {
      process.map(p => p.destroy())
      process = None
    }

  }
}