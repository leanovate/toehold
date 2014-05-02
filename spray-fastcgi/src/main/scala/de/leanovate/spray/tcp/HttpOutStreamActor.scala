/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.spray.tcp

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import de.leanovate.akka.tcp.{WriteBuffer, PMSubscriber, PMPublisher}
import akka.util.ByteString
import de.leanovate.akka.tcp.PMSubscriber._
import spray.http.{ChunkedMessageEnd, HttpData, MessageChunk}
import spray.can.Http.ConnectionClosed
import de.leanovate.akka.tcp.PMSubscriber.Data

class HttpOutStreamActor(publisher: PMPublisher[ByteString], responder: ActorRef)
  extends Actor with ActorLogging with PMSubscriber[ByteString] {
  import HttpOutStreamActor._

  private val writeBuffer = new WriteBuffer(self.toString(), log)
  private var subscription: Subscription = NoSubscription

  override def preStart() {
    publisher.subscribe(this)
  }

  override def receive = {
    case ChunkAck =>
      writeBuffer.takeChunk() match {
        case None =>
          log.error(s"$self chunk ack without pending")
        case Some(chunks) if chunks.isEmpty =>
          if (log.isDebugEnabled) {
            log.debug(s"$self resume out stream")
          }
          subscription.requestMore()
        case Some(chunks) =>
          chunks.head match {
            case Data(data) =>
              if (log.isDebugEnabled) {
                log.debug(s"$self writing chunk ${data.length}")
              }
              responder ! MessageChunk(HttpData(data)).withAck(ChunkAck)
            case EOF  =>
              responder ! ChunkedMessageEnd.withAck(EofAck)
          }
      }
    case EofAck =>
      if (log.isDebugEnabled) {
        log.debug(s"$self closing connection (eof)")
      }
      context stop self
    case ev: ConnectionClosed =>
      if (log.isDebugEnabled) {
        log.debug(s"$self closing connection: $ev")
      }
      context stop self
  }

  override def onSubscribe(_subscription: Subscription) {
    subscription = _subscription
  }

  override def onNext(chunk: Chunk[ByteString]) {
    writeBuffer.appendChunk(chunk) match {
      case None =>
        chunk match {
          case Data(data) =>
            if (log.isDebugEnabled) {
              log.debug(s"$self writing chunk ${data.length}")
            }
            responder ! MessageChunk(HttpData(data)).withAck(ChunkAck)
          case EOF  =>
            responder ! ChunkedMessageEnd.withAck(EofAck)
        }
      case _ =>
    }
  }
}

object HttpOutStreamActor {
  def props(publisher: PMPublisher[ByteString], responder: ActorRef) =
    Props(classOf[HttpOutStreamActor], publisher, responder)

  case object ChunkAck

  case object EofAck
}