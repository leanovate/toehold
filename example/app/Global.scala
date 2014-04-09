import akka.actor.PoisonPill
import de.leanovate.akka.fastcgi.FCGIRequestActor
import play.api.Play._
import play.api.{Application, GlobalSettings}

import play.api.libs.concurrent.Akka

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    Akka.system.actorSelection("/user/fastcgiRequest") ! PoisonPill
  }

  override def onStart(app: Application) {
    val fastCGIHost = app.configuration.getString("fastcgi.host").getOrElse("localhost")

    val fastCGIPort = app.configuration.getInt("fastcgi.port").getOrElse(9001)

    Akka.system(app).actorOf(FCGIRequestActor.props(fastCGIHost, fastCGIPort), "fastcgiRequest")
  }
}
