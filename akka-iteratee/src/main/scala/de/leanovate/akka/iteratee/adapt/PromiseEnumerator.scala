package de.leanovate.akka.iteratee.adapt

import play.api.libs.iteratee.{Done, Iteratee, Enumerator}
import scala.concurrent.{ExecutionContext, Promise, Future}

class PromiseEnumerator[E](implicit ctx: ExecutionContext) extends Enumerator[E] {
  val initialIteratee = Promise[Iteratee[E, Any]]()
  val promiseDone = Promise[Unit]()

  private var currentIteratee = initialIteratee.future

  override def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]] = {

    initialIteratee.success(i)
    promiseDone.future.map(_ => i)
  }

  val promisedIteratee: Iteratee[E, Any] = Iteratee.flatten(initialIteratee.future)

  def finish() {
    promiseDone.success()
  }
}
