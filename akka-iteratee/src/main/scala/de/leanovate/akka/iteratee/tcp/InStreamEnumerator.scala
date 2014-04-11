package de.leanovate.akka.iteratee.tcp

import akka.util.ByteString
import play.api.libs.iteratee._
import scala.concurrent.{ExecutionContext, Future, Promise}
import akka.io.Tcp
import akka.actor.ActorRef

class InStreamEnumerator(connection: ActorRef)(implicit client: ActorRef, ctx: ExecutionContext)
  extends Enumerator[ByteString] with PMStream[ByteString] {
  private val initialIteratee = Promise[Iteratee[ByteString, _]]()

  private val resultIteratee = Promise[Iteratee[ByteString, _]]()

  private var currentIteratee = initialIteratee.future

  override def sendChunk(data: ByteString, resume: () => Unit) {

    feed(Input.El(data), resume)
  }

  override def sendEOF() {

    resultIteratee.completeWith(feed(Input.EOF, () => {}))
  }

  private def feed(input: Input[ByteString], resume: () => Unit): Future[Iteratee[ByteString, _]] = {

    currentIteratee = currentIteratee.flatMap {
      it =>
        it.pureFold {
          case Step.Cont(k) =>
            resume()
            k(input)
          case Step.Done(result, remain) =>
            Done(result, remain)

          case Step.Error(msg, remain) =>
            connection ! Tcp.Abort
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
