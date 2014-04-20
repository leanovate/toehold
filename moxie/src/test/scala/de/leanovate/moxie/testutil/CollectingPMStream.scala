/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.testutil

import de.leanovate.akka.tcp.PMStream
import de.leanovate.akka.tcp.PMStream._
import de.leanovate.akka.tcp.PMStream.Data

class CollectingPMStream[A] extends PMStream[A] {
  val dataSeq = Seq.newBuilder[A]

  var eof = false

  var lastCtrl: Control = NoControl

  override def send(chunk: Chunk[A], ctrl: Control) {

    lastCtrl = ctrl
    chunk match {
      case Data(data) =>
        if (!eof) {
          dataSeq += data
            ctrl.resume()
        }
      case EOF =>
        eof = true
    }
  }

  def result(): Seq[A] = dataSeq.result()

  def clear() {
    dataSeq.clear()
    lastCtrl = NoControl
  }

  def markResume() {

    lastCtrl.resume()
    lastCtrl = NoControl
  }

  def markAbort(msg:String) {
    lastCtrl.abort(msg)
    lastCtrl = NoControl
  }
}
