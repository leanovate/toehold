/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.server

import akka.actor.{ActorLogging, Props, Actor}
import akka.io.{Tcp, IO}
import java.net.InetSocketAddress

class MoxieServer(listenPort: Int) extends Actor with ActorLogging {
  import context.system

  IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress("localhost", listenPort))

  override def receive = {

    case b@Tcp.Bound(localAddress) =>
      log.info(s"Moxie listening on $localAddress")

    case Tcp.CommandFailed(bind: Tcp.Bind) =>
      log.info(s"Moxie failed to listen on ${bind.localAddress}")
      context stop self

    case c@Tcp.Connected(remote, local) =>
      if (log.isDebugEnabled) {
        log.debug(s"Moxie connection $remote -> $local")
      }
  }
}

object MoxieServer {
  def props(listenPort: Int) = Props(classOf[MoxieServer], listenPort)
}
