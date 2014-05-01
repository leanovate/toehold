/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.testutil

import de.leanovate.akka.tcp.PMSubscriber
import de.leanovate.akka.tcp.PMSubscriber._
import de.leanovate.akka.tcp.PMSubscriber.Data

class CollectingPMSubscriber[A] extends PMSubscriber[A] {
  val dataSeq = Seq.newBuilder[A]

  var eof = false

  private var subscription: Subscription = NoSubscription

  override def onSubscribe(_subscription: Subscription) {
    subscription = _subscription
    subscription.requestMore()
  }

  override def onNext(chunk: Chunk[A]) {

    chunk match {
      case Data(data) =>
        if (!eof) {
          dataSeq += data
            subscription.requestMore()
        }
      case EOF =>
        eof = true
    }
  }

  def result(): Seq[A] = dataSeq.result()

  def clear() {
    dataSeq.clear()
    subscription = NoSubscription
  }

  def markResume() {

    subscription.requestMore()
  }

  def markAbort(msg:String) {
    subscription.cancel(msg)
  }
}
