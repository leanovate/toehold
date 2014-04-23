/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import akka.actor._
import de.leanovate.akka.fastcgi.request._
import de.leanovate.akka.fastcgi.records.FCGIRecord
import akka.util.ByteString
import de.leanovate.akka.tcp.PMConsumer
import de.leanovate.akka.fastcgi.request.FCGIResponderSuccess
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import de.leanovate.akka.fastcgi.request.FCGIResponderError
import akka.actor.Terminated
import de.leanovate.akka.tcp.AttachablePMConsumer
import scala.concurrent.duration.FiniteDuration

class FCGIRequestActor(host: String, port: Int, inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration)
  extends Actor with ActorLogging {

  import context.dispatcher

  var openConnections: Int = 0

  context.system.eventStream.subscribe(self, classOf[FCGIResponderSuccess])

  override def receive = {

    case FCGIQueryStatus =>
      sender ! FCGIStatus(openConnections)

    case request: FCGIResponderRequest =>
      openConnections += 1
      newClient(request, sender)

    case deadResponse: FCGIResponderSuccess =>
      deadResponse.content.abort("Response went to deadLetter (most likely due to timeout)")

    case Terminated(client) =>
      if (log.isDebugEnabled) {
        log.debug(s"FCGIClient terminated $client")
      }
      openConnections -= 1
  }

  private def newClient(request: FCGIResponderRequest, target: ActorRef) = {

    val handler = new FCGIConnectionHandler {
      override def connected(outStream: PMConsumer[FCGIRecord]) = {

        request.writeTo(1, outStream)
      }

      override def headerReceived(statusCode: Int, statusLine: String, headers: Seq[(String, String)],
        in: AttachablePMConsumer[ByteString]) = {

        target ! FCGIResponderSuccess(statusCode, statusLine, headers, in)
      }

      override def connectionFailed() {

        target ! FCGIResponderError(s"Connection to FastCGI process $host:$port failed")
      }
    }

    val client = context.actorOf(FCGIClient.props(host, port, inactivityTimeout, suspendTimeout, handler))
    if (log.isDebugEnabled) {
      log.debug(s"New FCGIClient $client")
    }
    context.watch(client)
    client
  }
}

object FCGIRequestActor {
  def props(host: String, port: Int, inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration) =
    Props(classOf[FCGIRequestActor], host, port, inactivityTimeout, suspendTimeout)
}
