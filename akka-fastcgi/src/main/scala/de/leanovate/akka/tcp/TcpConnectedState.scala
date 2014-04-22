/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import akka.actor.{Cancellable, ActorLogging, Actor, ActorRef}
import akka.util.ByteString
import akka.io.Tcp
import akka.io.Tcp.{Register, Event, ConnectionClosed, Received}
import akka.event.LoggingAdapter
import de.leanovate.akka.tcp.PMStream.{EOF, Data, Control, Chunk}
import scala.concurrent.stm._
import java.net.InetSocketAddress
import scala.concurrent.duration._

/**
 * Helper for the connected state of an actor doing some sort of tcp communication.
 *
 * All the back-pressure handling happens here.
 */
trait TcpConnectedState extends ActorLogging {
  actor: Actor =>

  def idleTimeout: FiniteDuration

  val readDeadline = Ref[Option[Deadline]](None)

  val writeDeadline = Ref[Option[Deadline]](None)

  var tickGenerator: Option[Cancellable] = None

  def becomeConnected(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef, inStream: PMStream[ByteString], closeOnEof: Boolean): PMStream[ByteString] = {

    val (connected, outPMStream) = connectedState(remoteAddress, localAddress, connection, inStream, closeOnEof)

    connection ! Register(self)

    context become connected
    outPMStream
  }

  def connectedState(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef, inStream: PMStream[ByteString],
    closeOnEof: Boolean): (Actor.Receive, PMStream[ByteString]) = {

    val outPMStram = new OutPMStream(remoteAddress, localAddress, connection, closeOnEof)

    val connectionControl = new ConnectionControl(remoteAddress, localAddress, connection)

    def connected: Actor.Receive = {

      case Received(data) =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress receive chunk: ${data.length} bytes")
        }
        readDeadline.single.set(Some(Deadline.now + idleTimeout))
        // Unluckily there is a lot of suspend/resume ping-pong, depending on the underlying buffers, sendChunk
        // might actually be called before the resume. This will become much cleaner with akka 2.3 in pull-mode
        connection ! Tcp.SuspendReading

        inStream.send(PMStream.Data(data), connectionControl)
      case TcpConnectedState.WriteAck =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress inner write ack")
        }

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
        inStream.send(PMStream.EOF, PMStream.NoControl)
        tickGenerator.foreach(_.cancel())
        context stop self
    }

    scheduleTick()

    (connected, outPMStram)
  }

  private def scheduleTick() {

    tickGenerator = Some(context.system.scheduler
      .scheduleOnce(1 second, self, TcpConnectedState.Tick)(context.dispatcher))
  }

  private class ConnectionControl(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef) extends Control {
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

  private class OutPMStream(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef, closeOnEof: Boolean) extends PMStream[ByteString] {
    private val pending = Ref[Option[(Seq[Chunk[ByteString]], Control)]](None)

    def acknowledge() {

      def takeChunk(state: Option[(Seq[Chunk[ByteString]], Control)]): Option[(Seq[Chunk[ByteString]], Control)] =
        state match {
          case Some((chunks, _)) if chunks.isEmpty =>
            if (log.isDebugEnabled) {
              log.debug(s"$localAddress -> $remoteAddress take last control from buffer")
            }
            None
          case Some((chunks, ctrl)) =>
            if (log.isDebugEnabled) {
              log.debug(s"$localAddress -> $remoteAddress take chunk from buffer")
            }
            Some(chunks.drop(1), ctrl)
          case None =>
            if (log.isDebugEnabled) {
              log.debug(s"$localAddress -> $remoteAddress buffer empty")
            }
            None
        }
      pending.single.getAndTransform(takeChunk) match {
        case None =>
          log.error(s"$localAddress -> $remoteAddress write ack without pending")
        case Some((chunks, ctrl)) if chunks.isEmpty =>
          if (log.isDebugEnabled) {
            log.debug(s"$localAddress -> $remoteAddress resume out stream")
          }
          ctrl.resume()
        case Some((chunks, _)) =>
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

    override def send(chunk: Chunk[ByteString], ctrl: Control) {

      def appendChunk(state: Option[(Seq[Chunk[ByteString]], Control)]): Option[(Seq[Chunk[ByteString]], Control)] =
        state match {
          case Some((chunks, _)) =>
            if (log.isDebugEnabled) {
              log.debug(s"$localAddress -> $remoteAddress push chunk to buffer")
            }
            (chunks, chunk) match {
              case (Seq(Data(remain)), Data(next)) =>
                Some(Seq(Data(remain ++ next)), ctrl)
              case _ =>
                Some(chunks :+ chunk, ctrl)
            }
          case None =>
            if (log.isDebugEnabled) {
              log.debug(s"$localAddress -> $remoteAddress push control to buffer")
            }
            Some(Seq.empty, ctrl)
        }

      pending.single.getAndTransform(appendChunk) match {
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
  }

}

object TcpConnectedState {

  private[tcp] case object WriteAck extends Event

  private[tcp] case object Tick

}