package de.leanovate.spray

import akka.actor.Actor
import spray.http.{StatusCodes, HttpResponse, Uri, HttpRequest}
import spray.http.HttpMethods._
import spray.can.Http

class Handler extends Actor {
  override def receive = {
    case _: Http.Connected =>
      sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      sender ! HttpResponse(entity = "PONG")
    case a =>
      println(a)
      sender ! HttpResponse(status = StatusCodes.NotFound)
  }
}
