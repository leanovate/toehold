/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.testutil

import de.leanovate.akka.tcp.PMStream
import de.leanovate.akka.tcp.PMStream.{EOF, Data, Control, Chunk}

class CollectingPMStream[A] extends PMStream[A] {
  val dataSeq = Seq.newBuilder[A]

  var eof = false

  override def send(chunk: Chunk[A], ctrl: Control) {

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
}
