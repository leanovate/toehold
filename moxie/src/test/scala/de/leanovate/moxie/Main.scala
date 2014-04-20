package de.leanovate.moxie

import akka.actor.ActorSystem
import de.leanovate.moxie.server.MoxieServer

object Main {
  def main(args:Array[String]) {
    val system = ActorSystem("Main")

    system.actorOf(MoxieServer.props(9999))
  }
}
