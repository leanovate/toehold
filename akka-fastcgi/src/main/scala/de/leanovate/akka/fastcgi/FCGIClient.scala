package de.leanovate.akka.fastcgi

import akka.actor._
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import de.leanovate.akka.tcp._
import akka.util.ByteString
import de.leanovate.akka.fastcgi.framing.{Framing, HeaderExtractor}
import scala.concurrent.duration.{Deadline, FiniteDuration}
import de.leanovate.akka.fastcgi.request.FCGIRequest
import de.leanovate.akka.pool.PoolSupport
import de.leanovate.akka.fastcgi.request.FCGIResponderSuccess
import de.leanovate.akka.fastcgi.request.FCGIResponderError
import akka.io.Tcp.Connected
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import scala.Some
import de.leanovate.akka.tcp.AttachablePMSubscriber

class FCGIClient(remote: InetSocketAddress, val inactivityTimeout: FiniteDuration, val suspendTimeout: FiniteDuration)
  extends Actor with ActorLogging {

  import context.system

  var idCount = 1

  var currentRequest: Option[(FCGIRequest, ActorRef)] = None

  var connectedState: Option[TcpConnectedState] = None

  var idleDeadline: Deadline = _

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
      currentRequest.foreach {
        case (request, target) =>
          target ! FCGIResponderError(s"Connection to FastCGI process $remote failed", request.ref)
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

  def idle: Actor.Receive = {
    case request: FCGIRequest =>
      if (log.isDebugEnabled) {
        log.debug(connectedState.fold("")(state => state.localAddress + " -> " + state.remoteAddress) + " reuse connection")
      }
      currentRequest = Some(request, sender)

      val pipeline = createPipeline()

      connectedState.get.reconnect(pipeline)

      becomeConnected()

    case TcpConnectedState.Tick =>
      if (idleDeadline.isOverdue()) {
        if (log.isDebugEnabled)
          log.debug(connectedState.fold("")(state => state.localAddress + " -> " + state.remoteAddress) + s" idle connection has been idle >= $suspendTimeout")
        connectedState.foreach(_.abort())
      }
      connectedState.foreach(_.scheduleTick())

    case c: ConnectionClosed =>
      if (log.isDebugEnabled) {
        log.debug(connectedState.fold("")(state => state.localAddress + " -> " + state.remoteAddress) + s" idle connection closed: $c")
      }
      becomeDisconnected()
  }

  def disconnecting: Actor.Receive = {
    case request: FCGIRequest =>
      currentRequest = Some(request, sender)
    case c: ConnectionClosed =>
      if (log.isDebugEnabled) {
        log.debug(connectedState.fold("")(state => state.localAddress + " -> " + state.remoteAddress) + s" connection closed: $c")
      }
      if (currentRequest.isDefined) {
        IO(Tcp) ! Connect(remote)
        context become connecting
      } else {
        becomeDisconnected()
      }
  }

  def becomeIdle() {

    currentRequest = None
    connectedState.get.connection ! Tcp.ResumeReading
    idleDeadline = Deadline.now + suspendTimeout
    context.parent ! PoolSupport.IamIdle
    context become idle
  }

  def becomeConnected() {
    currentRequest.foreach(_._1.writeTo(idCount, keepAlive = true, PMProcessor.map[FCGIRecord, ByteString](_.encode) |> connectedState.get.outStream))
    idCount += 1
    if (idCount > 30000)
      idCount = 1

    context.parent ! PoolSupport.IamBusy
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
    context.parent ! PoolSupport.IamFree
    context become disconnected
  }

  def createPipeline() = {
    val inStream = new AttachablePMSubscriber[ByteString]
    val httpExtractor = new HeaderExtractor({
      (statusCode, statusLine, headers) =>
        currentRequest.foreach {
          case (request, target) =>
            target ! FCGIResponderSuccess(statusCode, statusLine, headers, inStream, request.ref)
        }
    })
    Framing.bytesToFCGIRecords |>
      Framing.filterStdOut(stderrToLog) |>
      PMProcessor.flatMapChunk(httpExtractor) |>
      PMProcessor.onEof[ByteString](becomeIdle) |> inStream
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