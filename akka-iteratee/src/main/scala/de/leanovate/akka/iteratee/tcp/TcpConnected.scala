/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.iteratee.tcp

import akka.actor.{Actor, ActorContext, ActorRef}
import akka.util.ByteString
import de.leanovate.akka.iteratee.tcp.PMStream.{EOF, Data, Chunk, Control}
import akka.io.Tcp
import akka.io.Tcp.{Event, ConnectionClosed, Received}
import akka.event.Logging

class TcpConnected(connection: ActorRef, inStream: PMStream[ByteString], closeOnEof: Boolean)
  (implicit context: ActorContext, self: ActorRef) {
  val log = Logging(context.system, context.self)

  def outStream: PMStream[ByteString] = OutPMStream

  val state: Actor.Receive = {

    {
      case Received(data) =>
        if (log.isDebugEnabled) {
          log.debug(s"Chunk: ${data.length} bytes")
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
  }

  private object OutPMStream extends PMStream[ByteString] {
    private var pending: Option[PMStream.Control] = None

    def acknowledge() {

      pending.foreach {
        ctrl =>
          if (log.isDebugEnabled) {
            log.debug("Resume out stream")
          }
          pending = None
          ctrl.resume()
      }
    }

    override def send(chunk: Chunk[ByteString], ctrl: Control) {

      chunk match {
        case Data(data) =>
          pending match {
            case None =>
              if (log.isDebugEnabled) {
                log.debug(s"Writing chunk ${data.length}")
              }
              pending = Some(ctrl)
              connection ! Tcp.Write(data, WriteAck)
            case _ =>
              log.debug("Invoked sendChunk before resume")
              ctrl.abort("Invoked sendChunk before resume")
          }
        case EOF =>
          if (closeOnEof) {
            if (log.isDebugEnabled) {
              log.debug(s"Closing connection")
            }
            connection ! Tcp.Close
          }
      }
    }
  }

  private object ConnectionControll extends Control {
    override def resume() {

      connection ! Tcp.ResumeReading
    }

    override def abort(msg: String) {

      log.error(s"Aborting connection: $msg")
      connection ! Tcp.Abort
    }
  }

  private case object WriteAck extends Event

}
