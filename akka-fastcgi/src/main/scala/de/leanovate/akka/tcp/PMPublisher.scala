/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import akka.util.ByteString
import de.leanovate.akka.tcp.PMSubscriber._
import scala.concurrent.{Promise, Future}
import de.leanovate.akka.tcp.PMSubscriber.Data

trait PMPublisher[A] {
  def subscribe(consumer: PMSubscriber[A])

  def abort(msg: String)
}

object PMPublisher {
  def collect(publisher: PMPublisher[ByteString]): Future[ByteString] = {
    val promise = Promise[ByteString]()

    publisher.subscribe(new PMSubscriber[ByteString] {
      private var data: ByteString = ByteString.empty
      private var subscription: Subscription = NoSubscription

      override def onSubscribe(_subscription: Subscription) {
        subscription = _subscription
        subscription.requestMore()
      }

      override def onNext(chunk: Chunk[ByteString]) {
        chunk match {
          case Data(chunk) =>
            data ++= chunk
          case EOF =>
            promise.success(data)
        }
        subscription.requestMore()
      }
    })
    promise.future
  }
}