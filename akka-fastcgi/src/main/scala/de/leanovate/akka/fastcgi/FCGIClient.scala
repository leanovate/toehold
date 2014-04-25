package de.leanovate.akka.fastcgi

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import de.leanovate.akka.tcp.{TcpConnectedState, AttachablePMSubscriber, PMProcessor}
import akka.util.ByteString
import de.leanovate.akka.fastcgi.framing.{Framing, HeaderExtractor}
import scala.concurrent.duration.FiniteDuration
import de.leanovate.akka.fastcgi.request.{FCGIResponderSuccess, FCGIResponderError, FCGIRequest}
import de.leanovate.akka.pool.PoolSupport

class FCGIClient(remote: InetSocketAddress, val inactivityTimeout: FiniteDuration, val suspendTimeout: FiniteDuration)
  extends Actor with ActorLogging {

  import context.system

  var currentRequest: Option[(FCGIRequest, ActorRef)] = None

  var connectedState: Option[TcpConnectedState] = None

  def receive = disconnected

  def disconnected: Actor.Receive = {
    case request: FCGIRequest =>
      currentRequest = Some(request, sender)
      IO(Tcp) ! Connect(remote)
      context become connecting
  }

  def connecting: Actor.Receive = {

    case c@CommandFailed(_: Connect) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connect failed: $c")
      }
      currentRequest.foreach(_._2 ! FCGIResponderError(s"Connection to FastCGI process $remote failed"))
      becomeDisconnected()

    case c@Connected(remoteAddress, localAddress) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connected $localAddress -> $remoteAddress")
      }
      val inStream = new AttachablePMSubscriber[ByteString]
      val httpExtractor = new HeaderExtractor({
        (statusCode, statusLine, headers) =>
          currentRequest.foreach(_._2 ! FCGIResponderSuccess(statusCode, statusLine, headers, inStream))
      })
      val pipeline =
        Framing.bytesToFCGIRecords |>
          Framing.filterStdOut(stderrToLog) |>
          PMProcessor.flatMapChunk(httpExtractor) |> inStream

      val state = new TcpConnectedState(sender, remoteAddress, localAddress,
        pipeline, becomeDisconnecting, becomeDisconnected, closeOnEof = false, inactivityTimeout, suspendTimeout, log)
      connectedState = Some(state)

      currentRequest.foreach(_._1.writeTo(1, PMProcessor.map[FCGIRecord, ByteString](_.encode) |> state.outStream))

      context.parent ! PoolSupport.IamBusy
      context become state.receive
  }

  def disconnecting: Actor.Receive = {
    case request: FCGIRequest =>
      currentRequest = Some(request, sender)
    case c: ConnectionClosed =>
      if (log.isDebugEnabled) {
        log.debug(s"connection closed: $c")
      }
      if (currentRequest.isDefined) {
        IO(Tcp) ! Connect(remote)
        context become connecting
      } else {
        becomeDisconnected()
      }
  }

  def becomeDisconnecting() {
    connectedState = None
    context become disconnecting
  }

  def becomeDisconnected() {
    connectedState = None
    context.parent ! PoolSupport.IamFree
    context become disconnected
  }

  private def stderrToLog(stderr: ByteString) {

    log.error(s"Stderr: ${stderr.utf8String}")
  }
}

object FCGIClient {
  def props(hostname: String, port: Int, inactivityTimeout: FiniteDuration, idleTimeout: FiniteDuration) =
    Props(classOf[FCGIClient], new InetSocketAddress(hostname, port), inactivityTimeout, idleTimeout)

  def props(remote: InetSocketAddress, inactivityTimeout: FiniteDuration, idleTimeout: FiniteDuration) =
    Props(classOf[FCGIClient], remote, inactivityTimeout, idleTimeout)
}