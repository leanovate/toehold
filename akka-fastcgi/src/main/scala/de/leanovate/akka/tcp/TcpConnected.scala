/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import akka.actor.{Actor, ActorContext, ActorRef}
import akka.util.ByteString
import akka.io.Tcp
import akka.io.Tcp.{Event, ConnectionClosed, Received}
import akka.event.Logging
import de.leanovate.akka.tcp.PMStream.{EOF, Data, Control, Chunk}
import scala.concurrent.stm._

class TcpConnected(connection: ActorRef, inStream: PMStream[ByteString], closeOnEof: Boolean)
  (implicit context: ActorContext, self: ActorRef) {
  val log = Logging(context.system, context.self)

  def becomeConnected: PMStream[ByteString] = {

    context become connected
    OutPMStream
  }

  def connected: Actor.Receive = {

    case Received(data) =>
      if (log.isDebugEnabled) {
        log.debug(s"Receive chunk: ${data.length} bytes")
      }
      // Unluckily there is a lot of suspend/resume ping-pong, depending on the underlying buffers, sendChunk
      // might actually be called before the resume. This will become much cleaner with akka 2.3 in pull-mode
      connection ! Tcp.SuspendReading

      inStream.send(PMStream.Data(data), ConnectionControll)
    case WriteAck =>
      if (log.isDebugEnabled) {
        log.debug("Inner write ack")
      }

      OutPMStream.acknowledge()
    case c: ConnectionClosed =>
      if (log.isDebugEnabled) {
        log.debug(s"Connection closed: $c")
      }
      inStream.send(PMStream.EOF, PMStream.EmptyControl)
      context stop self
  }

  private object OutPMStream extends PMStream[ByteString] {
    private val pending = Ref[Option[(Seq[Chunk[ByteString]], Control)]](None)

    def acknowledge() {

      def takeChunk(state: Option[(Seq[Chunk[ByteString]], Control)]): Option[(Seq[Chunk[ByteString]], Control)] =
        state match {
          case Some((chunks, _)) if chunks.isEmpty =>
            None
          case Some((chunks, ctrl)) =>
            Some(chunks.take(1), ctrl)
          case None =>
            None
        }
      pending.single.getAndTransform(takeChunk) match {
        case None =>
          log.error("Write ack without pending")
        case Some((chunks, ctrl)) if chunks.isEmpty =>
          if (log.isDebugEnabled) {
            log.debug("Resume out stream")
          }
          ctrl.resume()
        case Some((chunks, _)) =>
          chunks.head match {
            case Data(data) =>
              if (log.isDebugEnabled) {
                log.debug(s"Writing chunk ${data.length}")
              }
              connection ! Tcp.Write(data, WriteAck)
            case EOF if closeOnEof =>
              if (log.isDebugEnabled) {
                log.debug(s"Closing connection")
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
            Some(chunks :+ chunk, ctrl)
          case None =>
            Some(Seq.empty, ctrl)
        }

      pending.single.getAndTransform(appendChunk) match {
        case None =>
          chunk match {
            case Data(data) =>
              if (log.isDebugEnabled) {
                log.debug(s"Writing chunk ${data.length}")
              }
              connection ! Tcp.Write(data, WriteAck)
            case EOF if closeOnEof =>
              if (log.isDebugEnabled) {
                log.debug(s"Closing connection")
              }
              connection ! Tcp.Close
            case EOF =>
          }
        case _ =>
      }
    }
  }

  private object ConnectionControll extends Control {
    override def resume() {

      if (log.isDebugEnabled) {
        log.debug(s"Resome reading")
      }
      connection ! Tcp.ResumeReading
    }

    override def abort(msg: String) {

      log.error(s"Aborting connection: $msg")
      connection ! Tcp.Abort
    }
  }

  private case object WriteAck extends Event

}
