/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.tcp

import play.api.libs.iteratee.{Cont, Done, Iteratee, Input}
import scala.concurrent.Promise
import de.leanovate.akka.tcp.PMConsumer.{NoSubscription, EOF, Subscription, Data}
import de.leanovate.akka.tcp.PMConsumer
import scala.concurrent.stm.Ref

object IterateeAdapter {
  def adapt[A](target: PMConsumer[A]): Iteratee[A, Unit] = {

    val lastPromise = Ref[Option[Promise[Iteratee[A, Unit]]]](None)

    target.onSubscribe(new Subscription {
      override def resume() {

        lastPromise.single.swap(None).foreach(_.success(Cont[A, Unit](step)))
      }

      override def abort(msg: String) {

        lastPromise.single.swap(None).foreach(_.failure(new RuntimeException(msg)))
      }
    })

    def step(i: Input[A]): Iteratee[A, Unit] = i match {

      case Input.EOF =>
        target.onNext(EOF)
        Done(Unit, Input.EOF)
      case Input.Empty =>
        Cont[A, Unit](step)
      case Input.El(e) =>

        val promise = Promise[Iteratee[A, Unit]]()
        lastPromise.single.swap(Some(promise))
          .foreach(_.failure(new RuntimeException("Iterator advanced before resume")))
        target.onNext(Data(e))
        Iteratee.flatten(promise.future)
    }

    Cont[A, Unit](step)
  }
}
