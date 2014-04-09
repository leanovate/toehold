package de.leanovate.play.fastcgi

import play.api.{Application, GlobalSettings}
import play.api.libs.concurrent.Akka
import de.leanovate.akka.fastcgi.FCGIRequestActor
import akka.actor.PoisonPill

trait FastCGISupport extends GlobalSettings {
  override def onStop(app: Application) {
    val fastCGIActor = Akka.system(app).actorSelection("/user/" + FastCGISupport.FASTCGI_ACTOR_NAME)

    fastCGIActor ! PoisonPill
  }

  override def onStart(app: Application) {

    val fastCGIHost = app.configuration.getString("fastcgi.host").getOrElse("localhost")

    val fastCGIPort = app.configuration.getInt("fastcgi.port").getOrElse(9001)

    Akka.system.actorOf(FCGIRequestActor.props(fastCGIHost, fastCGIPort), FastCGISupport.FASTCGI_ACTOR_NAME)
  }
}

object FastCGISupport {
  val FASTCGI_ACTOR_NAME = "fastcgiRequest"

}