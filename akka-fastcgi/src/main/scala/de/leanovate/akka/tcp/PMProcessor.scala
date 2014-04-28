/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMSubscriber._
import de.leanovate.akka.tcp.PMSubscriber.Data

trait PMProcessor[From, To] {
  def |>(target: PMSubscriber[To]): PMSubscriber[From]

  def |>[A](other: PMProcessor[To, A]) = new PMProcessor.ConcatProcessor(this, other)
}

object PMProcessor {
  def map[From, To](f: From => To) = new PMProcessor[From, To] {
    override def |>(target: PMSubscriber[To]) = new PMSubscriber[From] {
      private var subscription: Subscription = NoSubscription

      override def onSubscribe(_subscription: Subscription) {

        subscription = _subscription
        target.onSubscribe(subscription)
      }

      override def onNext(chunk: Chunk[From]) {

        chunk match {
          case Data(data) =>
            try {
              target.onNext(Data(f(data)))
            } catch {
              case e: Exception =>
                subscription.cancel(e.getMessage)
            }
          case EOF =>
            target.onNext(EOF)
        }
      }
    }
  }

  def mapChunk[From, To](f: Chunk[From] => Chunk[To]) = new PMProcessor[From, To] {
    override def |>(target: PMSubscriber[To]) = new PMSubscriber[From] {
      private var subscription: Subscription = NoSubscription

      override def onSubscribe(_subscription: Subscription) {

        subscription = _subscription
        target.onSubscribe(subscription)
      }

      override def onNext(chunk: Chunk[From]) =
        try {
          target.onNext(f(chunk))
        } catch {
          case e: Exception =>
            subscription.cancel(e.getMessage)
        }
    }
  }

  def flatMapChunk[From, To](f: Chunk[From] => Seq[Chunk[To]]) = new PMProcessor[From, To] {
    override def |>(target: PMSubscriber[To]) = new PMSubscriber[From] {
      private var subscription: Subscription = NoSubscription

      override def onSubscribe(_subscription: Subscription) {

        subscription = _subscription
        target.onSubscribe(subscription)
      }

      override def onNext(chunk: Chunk[From]) = {

        try {
          val result = f(chunk)
          if (result.isEmpty) {
            subscription.requestMore()
          } else {
            result.foreach(target.onNext)
          }
        } catch {
          case e: Exception =>
            subscription.cancel(e.getMessage)
        }
      }
    }
  }

  def onEof[T](onEof: () => Unit) = new PMProcessor[T, T] {
    override def |>(target: PMSubscriber[T]) = new PMSubscriber[T] {
      override def onSubscribe(subscription: Subscription) = target.onSubscribe(subscription)

      override def onNext(chunk: Chunk[T]) = chunk match {
        case EOF =>
          onEof()
          target.onNext(EOF)
        case _ =>
          target.onNext(chunk)
      }
    }
  }

  class ConcatProcessor[From, Mid, To](in: PMProcessor[From, Mid], out: PMProcessor[Mid, To])
    extends PMProcessor[From, To] {
    override def |>(target: PMSubscriber[To]) = in |> (out |> target)
  }

}