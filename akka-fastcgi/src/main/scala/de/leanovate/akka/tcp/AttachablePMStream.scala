/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMStream.{EmptyControl, Control, Chunk}

case class AttachablePMStream[A]() extends PMStream[A] {
  @volatile
  private var target: PMStream[A] = null

  private val chunks = Seq.newBuilder[Chunk[A]]

  private var lastCtrl: Control = EmptyControl

  override def send(chunk: Chunk[A], ctrl: Control) {

    if (target ne null) {
      target.send(chunk, ctrl)
    }
    else {
      synchronized {
        if (target ne null) {
          target.send(chunk, ctrl)
        }
        else {
          chunks += chunk
          lastCtrl = ctrl
        }
      }
    }
  }

  def attach(_target: PMStream[A]) {

    synchronized {
      _target.send(chunks.result(), lastCtrl)
      target = _target
    }
  }

}
