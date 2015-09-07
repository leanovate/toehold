/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import javax.inject.{Singleton, Inject}

import play.api.{Configuration, Environment, Plugin, Application}
import akka.actor.{PoisonPill, ActorRef}
import play.api.inject.{Binding, ApplicationLifecycle, Module}
import play.api.libs.concurrent.Akka
import de.leanovate.akka.fastcgi.FCGIRequestActor
import play.api.Play._
import java.io.File
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import java.util.concurrent.TimeUnit

@Singleton
class FastCGIPlugin @Inject() (app: Application, lifecycle: ApplicationLifecycle) extends Module {
  val FASTCGI_ACTOR_NAME = "fastcgiRequest"

  private var _requestActor: Option[ActorRef] = None

  private var _settings: Option[FastCGISettings] = None

  def requestActor: ActorRef = _requestActor.getOrElse(sys.error("FastCGI plugin not started"))

  def settings: FastCGISettings = _settings.getOrElse(sys.error("FastCGI plugin not started"))

  // start the actor
  start()

  private def start() {

    lifecycle.addStopHook { () =>
      Future.successful(stop())
    }

    val confSettings =
      FastCGISettings(
        documentRoot = configuration.getString("fastcgi.documentRoot").fold(new File("./php"))(new File(_)),
        requestTimeout = app.configuration.getMilliseconds("fastcgi.requestTimeout")
          .fold(new Timeout(1.minute))(Timeout.apply),
        suspendTimeout = app.configuration.getMilliseconds("fastcgi.suspendTimeout").fold(20.seconds)
          (FiniteDuration.apply(_, TimeUnit.MILLISECONDS)),
        maxConnections = app.configuration.getInt("fastcgi.maxConnections").getOrElse(10),
        host = app.configuration.getString("fastcgi.host").getOrElse("localhost"),
        port = app.configuration.getInt("fastcgi.port").getOrElse(9001),
        fileWhiteList = app.configuration.getStringList("fastcgi.assets.whitelist")
          .fold(Set("gif", "png", "js", "css", "jpg"))(_.toSet)
      )
    _settings = Some(confSettings)
    _requestActor = Some(Akka.system(app)
      .actorOf(FCGIRequestActor
      .props(confSettings.host, confSettings.port,
        confSettings.requestTimeout.duration, confSettings.suspendTimeout, confSettings.maxConnections),
        FASTCGI_ACTOR_NAME))
  }

  private def stop() {

    _requestActor.foreach(_ ! PoisonPill)
    _requestActor = None

    _settings = None
  }

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq()
  }
}

object FastCGIPlugin {

  private val instance = Application.instanceCache[FastCGIPlugin]

  def requestActor(implicit app: Application): ActorRef = instance(app).requestActor

  def settings(implicit app: Application): FastCGISettings = instance(app).settings
}
