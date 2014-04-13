/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import akka.actor._
import de.leanovate.akka.fastcgi.request.{FCGIResponderSuccess, FCGIResponderError, FCGIResponderRequest}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import akka.actor.Terminated
import akka.util.ByteString
import de.leanovate.akka.tcp.{EnumeratorAdapter, AttachablePMStream, IterateeAdapter, PMStream}

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
      override def connected(outStream: PMStream[FCGIRecord]) = {

        request.writeTo(1, IterateeAdapter.adapt(outStream))
      }

      override def headerReceived(statusCode: Int, statusLine: String, headers: Seq[(String, String)],
        in: AttachablePMStream[ByteString]) = {

        target ! FCGIResponderSuccess(statusCode, statusLine, headers, EnumeratorAdapter.adapt(in))
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
