package de.leanovate.akka.fastcgi

import akka.actor._
import de.leanovate.akka.fastcgi.request.{FCGIResponderSuccess, FCGIResponderError, FCGIResponderRequest}
import play.api.libs.iteratee.{Iteratee, Enumerator}
import de.leanovate.akka.fastcgi.records.{Framing, FCGIRecord}
import akka.actor.Terminated
import de.leanovate.akka.iteratee.adapt.PromiseEnumerator
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

  private def stderrToLog(stderr:ByteString) {
    log.error(s"Stderr: ${stderr.utf8String}")
  }

  private def newClient(request: FCGIResponderRequest, target: ActorRef) = {

    val handler = new FCGIConnectionHandler {
      override def connectionFailed() {

        target ! FCGIResponderError(s"Connection to FastCGI process $host:$port failed")
      }

      override def connected(in: Enumerator[FCGIRecord], out: Iteratee[FCGIRecord, Unit]) = {

        val promiseOut = new PromiseEnumerator[ByteString]
        in |>> Framing.filterStdOut(stderrToLog) &> promiseOut.promisedIteratee

        request.records(1) |>> out

        target ! FCGIResponderSuccess(Seq(), promiseOut)
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
