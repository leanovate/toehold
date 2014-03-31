package de.leanovate.akka.iteratee.tcp

import akka.util.ByteString
import play.api.libs.iteratee._
import scala.concurrent.{ExecutionContext, Future, Promise}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import akka.io.Tcp
import akka.actor.ActorRef

class InStreamEnumerator(connection: ActorRef)(implicit client: ActorRef, ctx:ExecutionContext) extends Enumerator[ByteString] {
  private val initialIteratee = Promise[Iteratee[ByteString, _]]()

  private val resultIteratee = Promise[Iteratee[ByteString, _]]()

  private var currentIteratee = initialIteratee.future

  private val pendingChunks = new AtomicInteger(0)

  private val suspended = new AtomicBoolean(false)

  def feedChunk(data: ByteString) {

    feed(Input.El(data), data.size)
  }

  def feedEOF() {

    resultIteratee.completeWith(feed(Input.EOF, 0))
  }

  private def feed(input: Input[ByteString], size: Int): Future[Iteratee[ByteString, _]] = {

    if (pendingChunks.addAndGet(size) > 128 * 1024) {
      if (suspended.compareAndSet(false, true)) {
        connection ! Tcp.SuspendReading
      }
    }
    currentIteratee = currentIteratee.flatMap {
      it =>
        it.pureFold {
          case Step.Cont(k) =>
            if (pendingChunks.addAndGet(-size) < 32 * 1024) {
              if (suspended.compareAndSet(true, false)) {
                connection ! Tcp.ResumeReading
              }
            }
            k(input)
          case Step.Done(result, remain) =>
            connection ! Tcp.Close
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
