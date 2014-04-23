/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMConsumer.{EOF, NoSubscription, Subscription, Chunk}

/**
 * Not yet attached stream.
 *
 * All incoming chunks will be buffered white the stream is unattached.
 */
case class AttachablePMConsumer[A]() extends PMConsumer[A] {
  @volatile
  private var target: PMConsumer[A] = null

  private val chunks = Seq.newBuilder[Chunk[A]]

  private var subscription: Subscription = NoSubscription

  override def onSubscribe(_subscription: Subscription) {

    synchronized {
      subscription = _subscription
      if (target ne null) {
        target.onSubscribe(subscription)
      }
    }
  }

  override def onNext(chunk: Chunk[A]) {

    if (target ne null) {
      target.onNext(chunk)
    } else {
      // this is not supposed to run below java 1.5, so double-check is ok
      synchronized {
        if (target ne null) {
          target.onNext(chunk)
        } else {
          chunks += chunk
        }
      }
    }
  }

  def attach(_target: PMConsumer[A]) {

    synchronized {
      _target.onSubscribe(subscription)
      chunks.result().foreach(_target.onNext)
      chunks.clear()
      target = _target
    }
  }

  def abort(msg: String) {

    synchronized {
      if (target ne null) {
        target.onNext(EOF)
      } else {
        subscription.abort(msg)
      }
    }
  }

}
