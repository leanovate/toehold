package de.leanovate.akka.iteratee.tcp

import akka.actor.ActorRef
import scala.concurrent.Promise
import play.api.libs.iteratee.{Done, Input, Cont, Iteratee}
import akka.io.Tcp
import akka.io.Tcp.Event
import akka.util.ByteString
import de.leanovate.akka.iteratee.tcp.PMStream.Control

class OutStreamAdapter[A](target: PMStream[A])(implicit client: ActorRef) {
  def iterator = Cont[A, Unit](step)

  private def step(i: Input[A]): Iteratee[A, Unit] = i match {

    case Input.EOF =>
      target.send(PMStream.EOF, PMStream.EmptyControl)
      Done(Unit, Input.EOF)
    case Input.Empty =>
      Cont[A, Unit](step)
    case Input.El(e) =>

      val promise = Promise[Iteratee[A, Unit]]()
      target.send(PMStream.Data(e), new Control {
        override def resume() = promise.success(Cont[A, Unit](step))

        override def abort(msg: String) = promise.failure(new RuntimeException(msg))
      })
      Iteratee.flatten(promise.future)
  }
}