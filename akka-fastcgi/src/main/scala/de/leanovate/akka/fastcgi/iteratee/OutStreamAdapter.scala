package de.leanovate.akka.fastcgi.iteratee

import akka.actor.ActorRef
import scala.concurrent.Promise
import play.api.libs.iteratee.{Done, Input, Cont, Iteratee}
import akka.io.Tcp
import akka.io.Tcp.Event

class OutStreamAdapter[A](connection: ActorRef, rawWriter: RawWriter[A], ackEvent: Event)(implicit client: ActorRef) {
  private var pending: Option[Promise[Iteratee[A, Unit]]] = None

  def iterator = Cont[A, Unit](step)

  def acknowledge() {

    pending match {
      case Some(promise) =>
        pending = None
        promise.success(Cont[A, Unit](step))
      case None =>
        throw new RuntimeException("acknowledge without pending write")
    }
  }

  private def step(i: Input[A]): Iteratee[A, Unit] = i match {

    case Input.EOF =>
      connection ! Tcp.Close
      Done(Unit, Input.EOF)
    case Input.Empty =>
      Cont[A, Unit](step)
    case Input.El(e) =>

      pending match {
        case None =>
          val promise = Promise[Iteratee[A, Unit]]()
          pending = Some(promise)
          connection ! Tcp.Write(rawWriter.write(e), ackEvent)
          Iteratee.flatten(promise.future)
        case Some(_) =>
          throw new RuntimeException("Write while still waiting for acknowledge")
      }
  }
}