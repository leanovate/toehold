/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import akka.actor.{Cancellable, ActorLogging, Actor, ActorRef}
import akka.util.ByteString
import akka.io.Tcp
import akka.io.Tcp.{Event, ConnectionClosed}
import de.leanovate.akka.tcp.PMConsumer._
import scala.concurrent.stm._
import java.net.InetSocketAddress
import scala.concurrent.duration._
import akka.io.Tcp.Register
import de.leanovate.akka.tcp.PMConsumer.Data
import scala.Some
import akka.io.Tcp.Received

/**
 * Helper for the connected state of an actor doing some sort of tcp communication.
 *
 * All the back-pressure handling happens here.
 */
trait TcpConnectedState extends ActorLogging {
  actor: Actor =>

  def idleTimeout: FiniteDuration

  def becomeDisconnected()

  val readDeadline = Ref[Option[Deadline]](None)

  val writeDeadline = Ref[Option[Deadline]](None)

  var tickGenerator: Option[Cancellable] = None

  def becomeConnected(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef, inStream: PMConsumer[ByteString], closeOnEof: Boolean): PMConsumer[ByteString] = {

    val (connected, outPMStream) = connectedState(remoteAddress, localAddress, connection, inStream, closeOnEof)

    connection ! Register(self)

    context become connected
    outPMStream
  }

  def connectedState(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef, inStream: PMConsumer[ByteString],
    closeOnEof: Boolean): (Actor.Receive, PMConsumer[ByteString]) = {

    val outPMStram = new OutPMConsumer(remoteAddress, localAddress, connection, closeOnEof)

    inStream.onSubscribe(new ConnectionSubscription(remoteAddress, localAddress, connection))

    def connected: Actor.Receive = {

      case Received(data) =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress receive chunk: ${data.length} bytes")
        }
        readDeadline.single.set(Some(Deadline.now + idleTimeout))
        // Unluckily there is a lot of suspend/resume ping-pong, depending on the underlying buffers, sendChunk
        // might actually be called before the resume. This will become much cleaner with akka 2.3 in pull-mode
        connection ! Tcp.SuspendReading

        inStream.onNext(PMConsumer.Data(data))

      case TcpConnectedState.WriteAck =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress inner write ack")
        }
        writeDeadline.single.set(None)

        outPMStram.acknowledge()

      case TcpConnectedState.Tick =>
        if (readDeadline.single.get.exists(_.isOverdue())) {
          log.error(s"$localAddress -> $remoteAddress read timeout")
          connection ! Tcp.Abort
        } else if (writeDeadline.single.get.exists(_.isOverdue())) {
          log.error(s"$localAddress -> $remoteAddress read timeout")
          connection ! Tcp.Abort
        }
        scheduleTick()

      case c: ConnectionClosed =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress connection closed: $c")
        }
        inStream.onNext(PMConsumer.EOF)
        tickGenerator.foreach(_.cancel())
        becomeDisconnected()
    }

    scheduleTick()

    (connected, outPMStram)
  }

  private def scheduleTick() {

    tickGenerator = Some(context.system.scheduler
      .scheduleOnce(1 second, self, TcpConnectedState.Tick)(context.dispatcher))
  }

  private class ConnectionSubscription(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef) extends Subscription {
    override def resume() {

      if (log.isDebugEnabled) {
        log.debug(s"$localAddress -> $remoteAddress resume reading")
      }
      readDeadline.single.set(None)
      connection ! Tcp.ResumeReading
    }

    override def abort(msg: String) {

      log.error(s"$localAddress -> $remoteAddress aborting connection: $msg")
      readDeadline.single.set(None)
      connection ! Tcp.Abort
    }
  }

  private class OutPMConsumer(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef, closeOnEof: Boolean) extends PMConsumer[ByteString] {
    private val writeBuffer = new WriteBuffer(remoteAddress, localAddress, log)

    private var subscription: Subscription = NoSubscription

    override def onSubscribe(_subscription: Subscription) {

      subscription = _subscription
    }

    override def onNext(chunk: Chunk[ByteString]) {

      writeBuffer.appendChunk(chunk) match {
        case None =>
          chunk match {
            case Data(data) =>
              if (log.isDebugEnabled) {
                log.debug(s"$localAddress -> $remoteAddress writing chunk ${data.length}")
              }
              writeDeadline.single.set(Some(Deadline.now + idleTimeout))
              connection ! Tcp.Write(data, TcpConnectedState.WriteAck)
            case EOF if closeOnEof =>
              if (log.isDebugEnabled) {
                log.debug(s"$localAddress -> $remoteAddress closing connection")
              }
              writeDeadline.single.set(None)
              connection ! Tcp.Close
            case EOF =>
          }
        case _ =>
      }
    }

    def acknowledge() {

      writeBuffer.takeChunk() match {
        case None =>
          log.error(s"$localAddress -> $remoteAddress write ack without pending")
        case Some(chunks) if chunks.isEmpty =>
          if (log.isDebugEnabled) {
            log.debug(s"$localAddress -> $remoteAddress resume out stream")
          }
          subscription.resume()
        case Some(chunks) =>
          chunks.head match {
            case Data(data) =>
              if (log.isDebugEnabled) {
                log.debug(s"$localAddress -> $remoteAddress writing chunk ${data.length}")
              }
              writeDeadline.single.set(Some(Deadline.now + idleTimeout))
              connection ! Tcp.Write(data, TcpConnectedState.WriteAck)
            case EOF if closeOnEof =>
              if (log.isDebugEnabled) {
                log.debug(s"$localAddress -> $remoteAddress closing connection")
              }
              writeDeadline.single.set(None)
              connection ! Tcp.Close
            case EOF =>
          }
      }
    }

  }

}

object TcpConnectedState {

  private[tcp] case object WriteAck extends Event

  private[tcp] case object Tick

}