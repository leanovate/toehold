/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.tcp

import play.api.libs.iteratee._
import scala.concurrent.{ExecutionContext, Future, Promise}
import de.leanovate.akka.tcp.PMStream.{EOF, Data, Chunk, Control}
import de.leanovate.akka.tcp.{PMStream, AttachablePMStream}

object EnumeratorAdapter {
  def adapt[A](attachable: AttachablePMStream[A])(implicit ctx: ExecutionContext): Enumerator[A] = new Enumerator[A] {
    private val resultIteratee = Promise[Iteratee[A, _]]()

    class IterateeStream(initial: Iteratee[A, _]) extends PMStream[A] {

      var currentIteratee = Future.successful(initial)

      override def send(chunk: Chunk[A], ctrl: Control) {

        chunk match {
          case Data(data) =>
            feed(Input.El(data), ctrl)
          case EOF =>
            resultIteratee.completeWith(feed(Input.EOF, PMStream.NoControl))
        }
      }

      private def feed(input: Input[A], ctrl: Control): Future[Iteratee[A, _]] = {

        currentIteratee = currentIteratee.flatMap {
          it =>
            it.pureFold {
              case Step.Cont(k) =>
                ctrl.resume()
                k(input)
              case Step.Done(result, remain) =>
                ctrl.resume()
                Done(result, remain)
              case Step.Error(msg, remain) =>
                ctrl.abort(msg)
                Error(msg, remain)
            }
        }
        currentIteratee
      }
    }

    override def apply[U](i: Iteratee[A, U]) = {

      attachable.attach(new IterateeStream(i))

      resultIteratee.future.asInstanceOf[Future[Iteratee[A, U]]]
    }
  }
}
