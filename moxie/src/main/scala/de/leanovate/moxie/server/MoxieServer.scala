/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.server

import akka.actor.{ActorLogging, Props, Actor}

class MoxieServer(listenPort: Int) extends Actor with ActorLogging {
  override def receive = ???
}

object MoxieServer {
  def props(listenPort: Int) = Props(classOf[MoxieServer], listenPort)
}
