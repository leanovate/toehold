/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.tcp

import play.api.libs.iteratee._
import scala.concurrent.{ExecutionContext, Future, Promise}
import de.leanovate.akka.tcp.PMSubscriber._
import de.leanovate.akka.tcp.PMSubscriber
import de.leanovate.akka.tcp.PMSubscriber.Data
import de.leanovate.akka.tcp.AttachablePMSubscriber

object EnumeratorAdapter {
  def adapt[A](attachable: AttachablePMSubscriber[A])(implicit ctx: ExecutionContext): Enumerator[A] = new Enumerator[A] {
    private val resultIteratee = Promise[Iteratee[A, _]]()

    class IterateeSubscriber(initial: Iteratee[A, _]) extends PMSubscriber[A] {

      private var currentIteratee = Future.successful(initial)

      private var subscription: Subscription = NoSubscription

      override def onSubscribe(_subscription: Subscription) {

        subscription = _subscription
      }

      override def onNext(chunk: Chunk[A]) {

        chunk match {
          case Data(data) =>
            feed(Input.El(data))
          case EOF =>
            resultIteratee.completeWith(feed(Input.EOF))
        }
      }

      private def feed(input: Input[A]): Future[Iteratee[A, _]] = {

        currentIteratee = currentIteratee.flatMap {
          it =>
            it.pureFold {
              case Step.Cont(k) =>
                subscription.requestMore()
                k(input)
              case Step.Done(result, remain) =>
                subscription.requestMore()
                Done(result, remain)
              case Step.Error(msg, remain) =>
                subscription.cancel(msg)
                Error(msg, remain)
            }
        }
        currentIteratee
      }
    }

    override def apply[U](i: Iteratee[A, U]) = {

      attachable.subscribe(new IterateeSubscriber(i))

      resultIteratee.future.asInstanceOf[Future[Iteratee[A, U]]]
    }
  }
}
