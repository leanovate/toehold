package de.leanovate.toehold.sbt

import sbt._
import sbt.Keys._
import sbtfilter.Plugin.Filter
import play.PlayRunHook
import play.Project._
import scala.Some

object PhpFpmPlugin extends Plugin {
  val phpFpmExecutables = SettingKey[Seq[String]]("php-fpm-executables")

  val phpFpmConfig = TaskKey[File]("php-fpm-config")

  val phpFpmProcess = AttributeKey[(Process, Thread)]("php-fpm-process")

  val phpFpmBaseDirectory = SettingKey[File]("php-fpm-base-directory")

  val phpFpmListenPort = SettingKey[Int]("php-fpm-listen-port")

  val phpIncludePath = SettingKey[String]("php-include-path")

  def phpFpmSettings =
    Seq(
         phpFpmExecutables := Seq("/usr/sbin/php-fpm", "/usr/sbin/php5-fpm"),

         phpFpmBaseDirectory <<= baseDirectory,

         phpFpmListenPort := 9110,

         phpIncludePath := ".",

         phpFpmConfig <<= (streams, phpFpmBaseDirectory, phpFpmListenPort, phpIncludePath) map {
           (s, base, listenPort, includePath) =>
             val prefix = base / "target" / "php-fpm"
             val dest = prefix / "php-fpm.conf"
             val props = Seq(
                              "baseDirectory" -> base.getAbsolutePath,
                              "prefix" -> prefix.getAbsolutePath,
                              "listenPort" -> listenPort.toString,
                              "includePath" -> includePath
                            ).toMap
             IO.transfer(getClass.getResourceAsStream("php-fpm.conf"), dest)
             Filter(s.log, Seq(dest), props)
             dest
         },

         commands <++= (baseDirectory, phpFpmExecutables) {
           (base, phpFpmExecs) => Seq(startPhpFpmCommand(base, phpFpmExecs), stopPhpFpmProcess(base))
         },

         playRunHooks <+= (state, streams, baseDirectory, phpFpmExecutables, phpFpmConfig).map {
           (state, stream, base, phpFpmExecs, config) => phpFpmRunHook(state, stream.log, base, phpFpmExecs, config)
         }
       )

  def createPhpFpmProcess(log: Logger, base: File, phpFpmExecs: Seq[String], config: File, args: String*) = {
    phpFpmExecs.find(new File(_).canExecute).map {
      phpFpmExec =>
        log.info(s"Running $phpFpmExec --fpm-config ${config.getAbsolutePath} ${args.mkString(" ")}")
        Process(phpFpmExec :: "--fpm-config" :: config.getAbsolutePath :: args.toList, base)
    }
  }

  def startPhpFpmCommand(base: File, phpFpmExecs: Seq[String]): Command = Command.args("start-php-fpm", "<args>") {
    (state, args) =>
      state.get(phpFpmProcess).fold {
        try {
          val project = Project.extract(state)
          val (nextState, config) = project.runTask(phpFpmConfig, state)
          createPhpFpmProcess(state.log, base, phpFpmExecs, config, args: _*).fold {
            state.log.warn(s"No executable found for php-fpm")
            state
          } {
            processBuilder =>
              val process = processBuilder.run()
              val terminator = new Thread {
                override def run() {
                  process.destroy()
                }
              }
              java.lang.Runtime.getRuntime.addShutdownHook(terminator)

              nextState.put(phpFpmProcess, (process, terminator))
          }
        } catch {
          case e: Exception =>
            state.log.warn(s"Unable to start php-fpm process: ${e.getMessage}")
            state
        }
      } {
        _ =>
          state.log.info("Already started")
          state
      }
  }

  def stopPhpFpmProcess(base: File): Command = Command.args("stop-php-fpm", "<>") {
    (state, args) =>
      state.get(phpFpmProcess).fold(state) {
        case (process, terminator) =>
          process.destroy()
          java.lang.Runtime.getRuntime.removeShutdownHook(terminator)
          state.remove(phpFpmProcess)
      }
  }

  def phpFpmRunHook(state: State, log: Logger, base: File, phpFpmExecs: Seq[String], config: File) = new PlayRunHook {

    var processAndTerminator: Option[(Process, Thread)] = None

    override def beforeStarted() = {

      state.get(phpFpmProcess).getOrElse {
        try {
          createPhpFpmProcess(state.log, base, phpFpmExecs, config).fold {
            state.log.warn(s"No executable found for php-fpm")
            state
          } {
            processBuilder =>
              val process = processBuilder.run()
              val terminator = new Thread {
                override def run() {
                  process.destroy()
                }
              }
              java.lang.Runtime.getRuntime.addShutdownHook(terminator)
              processAndTerminator = Some(process, terminator)
              state
          }
        } catch {
          case e: Exception =>
            state.log.warn(s"Unable to start php-fpm process: ${e.getMessage}")
            state
        }
      }
    }

    override def afterStopped(): Unit = {

      processAndTerminator.map {
        case (process, terminator) =>
          process.destroy()
          java.lang.Runtime.getRuntime.removeShutdownHook(terminator)
      }
      processAndTerminator = None
    }
  }
}
