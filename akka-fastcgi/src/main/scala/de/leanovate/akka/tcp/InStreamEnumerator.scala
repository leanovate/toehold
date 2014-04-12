package de.leanovate.akka.tcp

import akka.util.ByteString
import play.api.libs.iteratee._
import scala.concurrent.{ExecutionContext, Future, Promise}
import akka.actor.ActorRef
import de.leanovate.akka.tcp.PMStream.{Control, EOF, Chunk, Data}

class InStreamEnumerator(implicit client: ActorRef, ctx: ExecutionContext)
  extends Enumerator[ByteString] with PMStream[ByteString] {
  private val initialIteratee = Promise[Iteratee[ByteString, _]]()

  private val resultIteratee = Promise[Iteratee[ByteString, _]]()

  private var currentIteratee = initialIteratee.future

  override def send(chunk: Chunk[ByteString], ctrl: Control) {

    chunk match {
      case Data(data) =>
        feed(Input.El(data), ctrl)
      case EOF =>
        resultIteratee.completeWith(feed(Input.EOF, PMStream.EmptyControl))
    }
  }

  private def feed(input: Input[ByteString], ctrl: Control): Future[Iteratee[ByteString, _]] = {

    currentIteratee = currentIteratee.flatMap {
      it =>
        it.pureFold {
          case Step.Cont(k) =>
            ctrl.resume()
            k(input)
          case Step.Done(result, remain) =>
            Done(result, remain)

          case Step.Error(msg, remain) =>
            ctrl.abort(msg)
            Error(msg, remain)
        }
    }
    currentIteratee
  }

  override def apply[A](i: Iteratee[ByteString, A]) = {

    initialIteratee.success(i)

    resultIteratee.future.asInstanceOf[Future[Iteratee[ByteString, A]]]
  }
}
