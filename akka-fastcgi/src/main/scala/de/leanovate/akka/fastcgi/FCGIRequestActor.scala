/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import akka.actor._
import de.leanovate.akka.fastcgi.request.{FCGIResponderSuccess, FCGIResponderError, FCGIResponderRequest}
import play.api.libs.iteratee.{Iteratee, Enumerator}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import akka.actor.Terminated
import akka.util.ByteString

class FCGIRequestActor(host: String, port: Int) extends Actor with ActorLogging {

  import context.dispatcher

  override def receive = {

    case request: FCGIResponderRequest =>
      newClient(request, sender)

    case Terminated(client) =>
      if (log.isDebugEnabled) {
        log.debug(s"FCGIClient terminated $client")
      }
  }

  private def newClient(request: FCGIResponderRequest, target: ActorRef) = {

    val handler = new FCGIConnectionHandler {
      override def connected(out: Iteratee[FCGIRecord, Unit]) = {

        request.writeTo(1, out)
      }

      override def headerReceived(statusCode: Int, statusLine: String, headers: Seq[(String, String)],
        in: Enumerator[ByteString]) = {

        target ! FCGIResponderSuccess(statusCode, statusLine, headers, in)
      }

      override def connectionFailed() {

        target ! FCGIResponderError(s"Connection to FastCGI process $host:$port failed")
      }
    }

    val client = context.actorOf(FCGIClient.props(host, port, handler))
    if (log.isDebugEnabled) {
      log.debug(s"New FCGIClient $client")
    }
    context.watch(client)
    client
  }
}

object FCGIRequestActor {
  def props(host: String, port: Int) = Props(classOf[FCGIRequestActor], host, port)
}
