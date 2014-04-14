/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.tcp

import play.api.libs.iteratee.{Cont, Done, Iteratee, Input}
import scala.concurrent.Promise
import de.leanovate.akka.tcp.PMStream.{NoControl, EOF, Control, Data}
import de.leanovate.akka.tcp.PMStream

object IterateeAdapter {
  def adapt[A](target:PMStream[A]): Iteratee[A, Unit] ={

    def step(i: Input[A]): Iteratee[A, Unit] = i match {

      case Input.EOF =>
        target.send(EOF, NoControl)
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
