/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.iteratee.tcp

import java.util.concurrent.atomic.AtomicInteger

/**
 * Pull-mode stream (aka poor man's "reactive" streaming).
 *
 * This is most certainly not the best api for this kind of task, it is just supposed to avoid Future-cascades.
 * Will most likely be removed once akka has its own reactive stream (which might happen with 2.4).
 */
trait PMStream[A] {
  /**
   * Send a chunk of data to the stream.
   *
   * For invokers:
   *   a) Avoid re-calling this before the stream has given its ok via `ctrl.resumt()`
   *   b) This method is not supposed to be thread-safe (i.e. chunks have a clearly defined order)
   * For implementors:
   *   a) Never ever perform a blocking operation,
   *   b) ensure that you call `ctrl.resume()` once you are ready for the next chunk (either directly of asynchronously
   */
  def sendChunk(data: A, ctrl: PMStream.Control)

  /**
   * Send an EOF to the stream (i.e. no more chunks are coming)
   */
  def sendEOF()
}

object PMStream {

  trait Control {
    def resume()

    def abort(msg: String)
  }

  class CountdownResumer(ctrl: Control) extends Control {
    private val counter = new AtomicInteger(1)

    def increment() = counter.incrementAndGet()

    override def resume() {

      if (counter.decrementAndGet() <= 0) {
        ctrl.resume()
      }
    }

    override def abort(msg: String) {

      ctrl.abort(msg: String)
    }
  }

  object EmptyControl extends Control {
    override def resume() {}

    override def abort(msg: String) {}
  }

}