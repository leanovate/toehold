/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import play.api.Plugin
import play.api.Application
import akka.actor.{PoisonPill, ActorRef}
import play.api.libs.concurrent.Akka
import de.leanovate.akka.fastcgi.FCGIRequestActor
import play.api.Play._
import scala.Some
import java.io.File
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import java.util.concurrent.TimeUnit

class FastCGIPlugin(app: Application) extends Plugin {
  val FASTCGI_ACTOR_NAME = "fastcgiRequest"

  private var _requestActor: Option[ActorRef] = None

  private var _settings: Option[FastCGISettings] = None

  def requestActor: ActorRef = _requestActor.getOrElse(sys.error("FastCGI plugin not started"))

  def settings: FastCGISettings = _settings.getOrElse(sys.error("FastCGI plugin not started"))

  override def onStart() {

    val confSettings =
      FastCGISettings(
                       documentRoot = configuration.getString("fastcgi.documentRoot").map(new File(_))
                         .getOrElse(new File("./php")),
                       requestTimeout = app.configuration.getMilliseconds("fastcgi.requestTimeout")
                         .map(Timeout.apply).getOrElse(new Timeout(1.minute)),
                       suspendTimeout = app.configuration.getMilliseconds("fastcgi.suspendTimeout")
                         .map(FiniteDuration.apply(_, TimeUnit.MILLISECONDS)).getOrElse(20.seconds),
                       host = app.configuration.getString("fastcgi.host").getOrElse("localhost"),
                       port = app.configuration.getInt("fastcgi.port").getOrElse(9001),
                       fileWhiteList = app.configuration.getStringList("fastcgi.assets.whitelist")
                         .map(_.toSet).getOrElse(Set("gif", "png", "js", "css", "jpg"))
                     )
    _settings = Some(confSettings)
    _requestActor = Some(Akka.system(app)
      .actorOf(FCGIRequestActor
      .props(confSettings.host, confSettings.port, confSettings.requestTimeout.duration, confSettings.suspendTimeout),
        FASTCGI_ACTOR_NAME))
  }

  override def onStop() {

    _requestActor.foreach(_ ! PoisonPill)
    _requestActor = None

    _settings = None
  }

  override def enabled = !app.configuration.getString("fastcgi.plugin").filter(_ == "disabled").isDefined
}

object FastCGIPlugin extends Plugin {
  def requestActor(implicit app: Application): ActorRef =
    app.plugin[FastCGIPlugin].map(_.requestActor).getOrElse(sys.error("FastCGI plugin not registered"))

  def settings(implicit app: Application): FastCGISettings =
    app.plugin[FastCGIPlugin].map(_.settings).getOrElse(sys.error("FastCGI plugin not registered"))
}
