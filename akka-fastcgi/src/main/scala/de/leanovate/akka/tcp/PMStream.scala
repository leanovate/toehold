/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

/**
 * Pull-mode stream (aka poor man's "reactive" streaming).
 *
 * This is most certainly not the best api for this kind of task, it is just supposed to avoid Future-cascades.
 * Will most likely be removed once akka has its own reactive stream (which might happen with 2.4).
 *
 * You might ask: Why is this called pull-mode when there is a `push` method?
 * The idea is that the stream should always ba able to react to IO events, but has control when the input should be
 * resumed. In return this implies that the IO manager should suspend all further reading until the stream decides to
 * resume.
 */
trait PMStream[A] {

  import PMStream._

  /**
   * Send a chunk of data to the stream.
   *
   * For invokers:
   * a) Avoid re-calling this before the stream has given its ok via `ctrl.resumt()`
   * b) This method is not supposed to be thread-safe (i.e. chunks have a clearly defined order)
   * For implementors:
   * a) Never ever perform a blocking operation,
   * b) ensure that you call `ctrl.resume()` once you are ready for the next chunk (either directly of asynchronously
   */
  def send(chunk: Chunk[A], ctrl: Control)

  /**
   * Send a sequence of chunks.
   *
   * THis might happen during framing/un-framing. If you already have a sequence of chunks in memory you have to get
   * rid of them somehow.
   */
  def sendSeq(chunks: Seq[Chunk[A]], ctrl: Control) {

    if (chunks.isEmpty) {
      ctrl.resume()
    } else {
      val it = chunks.iterator
      while (it.hasNext) {
        val chunk = it.next()

        if (it.hasNext) {
          send(chunk, new Control {
            override def resume() {}

            override def abort(msg: String) = ctrl.abort(msg)
          })
        } else {
          send(chunk, ctrl)
        }
      }
    }
  }

  /**
   * Push some data to the stream.
   *
   * Note: This does not have any form of back-pressure handling. Use with care.
   */
  def push(data: A*) {
    sendSeq(data.map(Data.apply), NoControl)
  }
}

object PMStream {

  sealed trait Chunk[+A]

  case class Data[A](data: A) extends Chunk[A]

  case object EOF extends Chunk[Nothing]

  trait Control {
    def resume()

    def abort(msg: String)
  }

  object NoControl extends Control {
    override def resume() {}

    override def abort(msg: String) {}
  }

}