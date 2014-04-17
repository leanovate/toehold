package de.leanovate.play.fastcgi

import play.api.Plugin
import play.api.Application
import akka.actor.{PoisonPill, ActorRef}
import play.api.libs.concurrent.Akka
import de.leanovate.akka.fastcgi.FCGIRequestActor

class FastCGIPlugin(app: Application) extends Plugin {
  val FASTCGI_ACTOR_NAME = "fastcgiRequest"

  private var _fastCgiRequestActor: Option[ActorRef] = None

  def fastCgiRequestActor: ActorRef = _fastCgiRequestActor.getOrElse(sys.error("FastCGI plugin not started"))

  override def onStart() {

    val fastCGIHost = app.configuration.getString("fastcgi.host").getOrElse("localhost")

    val fastCGIPort = app.configuration.getInt("fastcgi.port").getOrElse(9001)

    _fastCgiRequestActor = Some(Akka.system(app)
      .actorOf(FCGIRequestActor.props(fastCGIHost, fastCGIPort), FASTCGI_ACTOR_NAME))
  }

  override def onStop() {

    _fastCgiRequestActor.foreach(_ ! PoisonPill)
    _fastCgiRequestActor = None
  }

  override def enabled = !app.configuration.getString("fastcgi.plugin").filter(_ == "disabled").isDefined
}

object FastCGIPlugin extends Plugin {
  def fastCgiRequestActor(implicit app: Application): ActorRef =
    app.plugin[FastCGIPlugin].map(_.fastCgiRequestActor).getOrElse(sys.error("FastCGI plugin not registers"))
}