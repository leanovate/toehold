package de.leanovate.akka.tcp

import play.api.libs.iteratee.{Cont, Done, Iteratee, Input}
import scala.concurrent.Promise
import de.leanovate.akka.tcp.PMStream.{EmptyControl, EOF, Control, Data}

object IterateeAdapter {
  def adapt[A](target:PMStream[A]): Iteratee[A, Unit] ={

    def step(i: Input[A]): Iteratee[A, Unit] = i match {

      case Input.EOF =>
        target.send(EOF, EmptyControl)
        Done(Unit, Input.EOF)
      case Input.Empty =>
        Cont[A, Unit](step)
      case Input.El(e) =>

        val promise = Promise[Iteratee[A, Unit]]()
        target.send(Data(e), new Control {
          override def resume() = promise.success(Cont[A, Unit](step))

          override def abort(msg: String) = promise.failure(new RuntimeException(msg))
        })
        Iteratee.flatten(promise.future)
    }

    Cont[A, Unit](step)
  }
}
