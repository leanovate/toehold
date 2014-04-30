package de.leanovate.spray

import akka.actor.Actor
import spray.http.{HttpResponse, Uri, HttpRequest}
import spray.http.HttpMethods._
import spray.can.Http
import de.leanovate.spray.fastcgi.FastCGISupport

class Handler extends Actor with FastCGISupport {
  override def receive = primary.orElse(fastcgiReceive)

  def primary: Actor.Receive = {

    case _: Http.Connected =>
      sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      sender ! HttpResponse(entity = "PONG")
  }
}
