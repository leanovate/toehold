/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.util.ByteString
import akka.io.Tcp
import akka.io.Tcp.{Event, ConnectionClosed, Received}
import akka.event.LoggingAdapter
import de.leanovate.akka.tcp.PMStream.{EOF, Data, Control, Chunk}
import scala.concurrent.stm._
import java.net.InetSocketAddress

/**
 * Helper for the connected state of an actor doing some sort of tcp communication.
 */
trait TcpConnectionActor extends ActorLogging {
  actor: Actor =>

  def becomeConnected(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
    connection: ActorRef, inStream: PMStream[ByteString], closeOnEof: Boolean): PMStream[ByteString] = {

    val outPMStram = new TcpConnectionActor.OutPMStream(remoteAddress, localAddress, log, connection, closeOnEof)

    val connectionControl = new TcpConnectionActor.ConnectionControl(remoteAddress, localAddress, log, connection)

    def connected: Actor.Receive = {

      case Received(data) =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress receive chunk: ${data.length} bytes")
        }
        // Unluckily there is a lot of suspend/resume ping-pong, depending on the underlying buffers, sendChunk
        // might actually be called before the resume. This will become much cleaner with akka 2.3 in pull-mode
        connection ! Tcp.SuspendReading

        inStream.send(PMStream.Data(data), connectionControl)
      case TcpConnectionActor.WriteAck =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress inner write ack")
        }

        outPMStram.acknowledge()
      case c: ConnectionClosed =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress connection closed: $c")
        }
        inStream.send(PMStream.EOF, PMStream.EmptyControl)
        context stop self
    }

    context become connected
    outPMStram
  }
}

object TcpConnectionActor {

  private class ConnectionControl(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress, log: LoggingAdapter, connection: ActorRef)(implicit sender: ActorRef)
    extends Control {
    override def resume() {

      if (log.isDebugEnabled) {
        log.debug(s"$localAddress -> $remoteAddress resome reading")
      }
      connection ! Tcp.ResumeReading
    }

    override def abort(msg: String) {

      log.error(s"$localAddress -> $remoteAddress aborting connection: $msg")
      connection ! Tcp.Abort
    }
  }

  private class OutPMStream(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress, log: LoggingAdapter, connection: ActorRef, closeOnEof: Boolean)(implicit sender: ActorRef)
    extends PMStream[ByteString] {
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
              connection ! Tcp.Write(data, WriteAck)
            case EOF if closeOnEof =>
              if (log.isDebugEnabled) {
                log.debug(s"$localAddress -> $remoteAddress closing connection")
              }
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
            Some(chunks :+ chunk, ctrl)
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
              connection ! Tcp.Write(data, WriteAck)
            case EOF if closeOnEof =>
              if (log.isDebugEnabled) {
                log.debug(s"$localAddress -> $remoteAddress closing connection")
              }
              connection ! Tcp.Close
            case EOF =>
          }
        case _ =>
      }
    }
  }

  private case object WriteAck extends Event

}