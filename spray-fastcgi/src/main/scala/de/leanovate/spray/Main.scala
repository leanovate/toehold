package de.leanovate.spray

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http

object Main extends App {
  implicit val system = ActorSystem()

  val listener = system.actorOf(Props[Handler])

  IO(Http) ! Http.Bind(listener, interface = "localhost", port = 8080)
}
