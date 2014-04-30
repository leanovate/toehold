package de.leanovate.spray.fastcgi

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
      requestTimeout = Timeout(system.settings.config.getMilliseconds("fastcgi.requestTimeout")),
      suspendTimeout = FiniteDuration(system.settings.config.getMilliseconds("fastcgi.suspendTimeout"), TimeUnit.MILLISECONDS),
      maxConnections = system.settings.config.getInt("fastcgi.maxConnections"),
      host = system.settings.config.getString("fastcgi.host"),
      port = system.settings.config.getInt("fastcgi.port"),
      fileWhiteList = system.settings.config.getStringList("fastcgi.assets.whitelist").toSet
    )

  val requestActor =
    system.actorOf(
      FCGIRequestActor
        .props(settings.host, settings.port,
          settings.requestTimeout.duration, settings.suspendTimeout, settings.maxConnections),
      FASTCGI_ACTOR_NAME)
}

object FastCGIExtension extends ExtensionId[FastCGIExtension] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem) =
    new FastCGIExtension(system)

  override def lookup() = this
}
