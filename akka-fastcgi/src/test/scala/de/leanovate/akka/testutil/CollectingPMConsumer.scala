/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.testutil

import de.leanovate.akka.tcp.PMConsumer
import de.leanovate.akka.tcp.PMConsumer._
import de.leanovate.akka.tcp.PMConsumer.Data

class CollectingPMConsumer[A] extends PMConsumer[A] {
  val dataSeq = Seq.newBuilder[A]

  var eof = false

  private var subscription: Subscription = NoSubscription

  override def onSubscribe(_subscription: Subscription) {

    subscription = _subscription
  }

  override def onNext(chunk: Chunk[A]) {

    chunk match {
      case Data(data) =>
        if (!eof) {
          dataSeq += data
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

    subscription.resume()
  }

  def markAbort(msg:String) {
    subscription.abort(msg)
  }
}
