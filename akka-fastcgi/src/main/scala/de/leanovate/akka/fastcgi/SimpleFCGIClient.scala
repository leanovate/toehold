package de.leanovate.akka.fastcgi

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import de.leanovate.akka.fastcgi.request.{FCGIRequestWithRemote, FCGIResponderSuccess, FCGIResponderError, FCGIRequest}
import akka.io.{Tcp, IO}
import akka.io.Tcp.{ConnectionClosed, Connected, CommandFailed, Connect}
import scala.concurrent.duration.FiniteDuration
import de.leanovate.akka.tcp.{PMProcessor, AttachablePMSubscriber, PMSubscriber, TcpConnectedState}
import akka.util.ByteString
import de.leanovate.akka.fastcgi.framing.Framing
import de.leanovate.akka.fastcgi.records.FCGIRecord

class SimpleFCGIClient(val inactivityTimeout: FiniteDuration, val suspendTimeout: FiniteDuration)
  extends Actor with ActorLogging {

  import context.system

  var idCount = 1

  var currentRequest: Option[(FCGIRequestWithRemote, ActorRef)] = None

  var connectedState: Option[TcpConnectedState] = None

  def receive = disconnected

  def disconnected: Actor.Receive = {
    case request: FCGIRequestWithRemote =>
      currentRequest = Some(request, sender)
      IO(Tcp) ! Connect(request.remote)
      context become connecting
  }

  def connecting: Actor.Receive = {

    case c@CommandFailed(_: Connect) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connect failed: $c")
      }
      currentRequest.foreach {
        case (request, target) =>
          target ! FCGIResponderError(s"Connection to FastCGI process ${request.remote} failed", request.ref)
      }
      becomeDisconnected()

    case c@Connected(remoteAddress, localAddress) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connected $localAddress -> $remoteAddress")
      }

      val pipeline = createPipeline()

      val state = new TcpConnectedState(sender, remoteAddress, localAddress,
        pipeline, becomeDisconnecting, becomeDisconnected, closeOnOutEof = false, inactivityTimeout, suspendTimeout, log)
      connectedState = Some(state)

      becomeConnected()
  }

  def disconnecting: Actor.Receive = {
    case c: ConnectionClosed =>
      if (log.isDebugEnabled) {
        log.debug(connectedState.fold("")(state => state.localAddress + " -> " + state.remoteAddress) + s" connection closed: $c")
      }
      becomeDisconnected()
  }

  def becomeConnected() {
    currentRequest.foreach(_._1.writeTo(idCount, keepAlive = false,
      PMProcessor.map[FCGIRecord, ByteString](_.encode) |> connectedState.get.outStream))
    idCount += 1
    if (idCount > 30000)
      idCount = 1

    context become connectedState.get.receive
  }

  def becomeDisconnecting() {
    connectedState.foreach(_.deactivate())
    connectedState = None
    context become disconnecting
  }

  def becomeDisconnected() {
    connectedState.foreach(_.deactivate())
    connectedState = None
    context stop self
  }

  def createPipeline() = {
    val responseHeaderSubscriber = new ResponseHeaderSubscriber {
      override def onHeader(statusCode: Int, statusLine: String, headers: Seq[(String, String)]) = {
        currentRequest.fold(PMSubscriber.nullStream[ByteString]) {
          case (request, target) =>
            val inStream = new AttachablePMSubscriber[ByteString]
            target ! FCGIResponderSuccess(statusCode, statusLine, headers, inStream, request.ref)
            inStream
        }
      }
    }
    Framing.bytesToFCGIRecords |>
      Framing.filterStdOut(stderrToLog) |>
      PMProcessor.onEof[ByteString](becomeDisconnecting) |>
      responseHeaderSubscriber
  }

  private def stderrToLog(stderr: ByteString) {

    log.error(s"Stderr: ${stderr.utf8String}")
  }
}

object SimpleFCGIClient {
  def props(inactivityTimeout: FiniteDuration, idleTimeout: FiniteDuration) =
    Props(classOf[SimpleFCGIClient], inactivityTimeout, idleTimeout)
}