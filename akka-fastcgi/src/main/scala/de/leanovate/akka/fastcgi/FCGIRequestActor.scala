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

        request.records(1) |>> out
      }

      override def headerReceived(headers: Seq[(String, String)], in: Enumerator[ByteString]) = {

        target ! FCGIResponderSuccess(headers, in)
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
