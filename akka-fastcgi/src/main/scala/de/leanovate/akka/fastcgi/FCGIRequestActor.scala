package de.leanovate.akka.fastcgi

import akka.actor.{Props, Actor}
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest

class FCGIRequestActor(host:String, port:Int) extends Actor {
  override def receive = {
    case request:FCGIResponderRequest =>
  }
}

object FCGIRequestActor {
  def props(host:String, port:Int) = Props(classOf[FCGIRequestActor], host, port)
}
