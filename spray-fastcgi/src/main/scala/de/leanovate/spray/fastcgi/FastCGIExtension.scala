/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.spray.fastcgi

import java.util.concurrent.TimeUnit.MILLISECONDS

import akka.actor.{ExtensionIdProvider, ExtensionId, Extension, ExtendedActorSystem}
import java.io.File
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import de.leanovate.akka.fastcgi.FCGIRequestActor

class FastCGIExtension(system: ExtendedActorSystem) extends Extension {
  val FASTCGI_ACTOR_NAME = "fastcgiRequest"

  val settings =
    FastCGISettings(
      documentRoot = new File(system.settings.config.getString("fastcgi.documentRoot")),
      requestTimeout = FiniteDuration(system.settings.config.getDuration("fastcgi.requestTimeout", MILLISECONDS), MILLISECONDS),
      suspendTimeout = FiniteDuration(system.settings.config.getDuration("fastcgi.suspendTimeout", MILLISECONDS), MILLISECONDS),
      maxConnections = system.settings.config.getInt("fastcgi.maxConnections"),
      host = system.settings.config.getString("fastcgi.host"),
      port = system.settings.config.getInt("fastcgi.port"),
      fileWhiteList = system.settings.config.getStringList("fastcgi.assets.whitelist").toSet
    )

  val requestActor =
    system.actorOf(
      FCGIRequestActor.props(settings.host, settings.port,
        settings.requestTimeout, settings.suspendTimeout, settings.maxConnections),
      FASTCGI_ACTOR_NAME)
}

object FastCGIExtension extends ExtensionId[FastCGIExtension] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem) =
    new FastCGIExtension(system)

  override def lookup() = this
}
