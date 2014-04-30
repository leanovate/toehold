package de.leanovate.spray.tcp

import de.leanovate.akka.tcp.PMSubscriber
import de.leanovate.akka.tcp.PMSubscriber.{Data, EOF, Subscription, Chunk}
import akka.util.ByteString
import akka.actor.ActorRef
import spray.http.{ChunkedMessageEnd, HttpData, MessageChunk}
import de.leanovate.spray.tcp.HttpOutPMSubscriber.WriteAck
import java.util.concurrent.atomic.AtomicInteger

class HttpOutPMSubscriber(connection: ActorRef) extends PMSubscriber[ByteString] {
  private val pending = new AtomicInteger(0)
  private var subscription: Option[Subscription] = None

  def ackknowledge() {
    if (pending.decrementAndGet() == 0)
      subscription.foreach(_.requestMore())
  }

  override def onSubscribe(_subscription: Subscription) {
    subscription = Some(_subscription)
  }

  override def onNext(chunk: Chunk[ByteString]) {
    chunk match {
      case Data(chunk) =>
        pending.incrementAndGet()
        connection ! MessageChunk(HttpData(chunk)).withAck(WriteAck)
      case EOF =>
        connection ! ChunkedMessageEnd
    }
  }
}

object HttpOutPMSubscriber {

  case object WriteAck
}