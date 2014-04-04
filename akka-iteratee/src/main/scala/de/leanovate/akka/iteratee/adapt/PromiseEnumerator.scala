package de.leanovate.akka.iteratee.adapt

import play.api.libs.iteratee.{Iteratee, Enumerator}
import scala.concurrent.{ExecutionContext, Promise, Future}

class PromiseEnumerator[E](implicit ctx: ExecutionContext) extends Enumerator[E] {
  val promise = Promise[Iteratee[E, Any]]()

  override def apply[A](i: Iteratee[E, A]): Future[Iteratee[E, A]] = {

    promise.success(i)
    Future.successful(i)
  }

  val promisedIteratee: Iteratee[E, Any] = Iteratee.flatten(promise.future)
}
