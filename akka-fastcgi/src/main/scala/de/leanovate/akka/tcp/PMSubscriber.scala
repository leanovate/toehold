/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

/**
 * Pull-mode subscriber (aka poor man's "reactive" streaming).
 *
 * This is most certainly not the best api for this kind of task, it is just supposed to avoid Future-cascades.
 * Overall the interface is quite similar to the SPI suggested by reactive-streams.org and therefore might be
 * easily replaced by akka-streams once they are available (maybe with akka 2.4)
 *
 * You might ask: Why is this called pull-mode when there is a `push` method?
 * The idea is that the stream should always ba able to react to IO events, but has control when the input should be
 * resumed. In return this implies that the IO manager should suspend all further reading until the stream decides to
 * resume.
 */
trait PMSubscriber[A] {

  import PMSubscriber._

  /**
   * Called once the consumer subscribes/attaches to some sort of producer.
   *
   * For invokers:
   * a) Ensure that this is called before everything else
   * b) Avoid re-calling `onNext` before consumer has given its ok via `subscription.resume()`
   * For implementors:
   * a) Never ever do a blocking operation here.
   */
  def onSubscribe(subscription: Subscription)

  /**
   * Send a chunk of data to the stream.
   *
   * For invokers:
   * a) Avoid re-calling this before the stream has given its ok via `subscription.resume()`
   * b) This method is not supposed to be thread-safe (i.e. chunks have a clearly defined order)
   * For implementors:
   * a) Never ever perform a blocking operation,
   * b) ensure that you call `ctrl.resume()` once you are ready for the next chunk (either directly of asynchronously
   */
  def onNext(chunk: Chunk[A])

  /**
   * Push some data to the stream.
   *
   * Note: This does not have any form of back-pressure handling. Use with care.
   */
  def push(data: A*) {
    data.map(Data.apply).foreach(onNext)
  }
}

object PMSubscriber {

  /**
   * Abstraction of a chunk transmitted to a stream.
   */
  sealed trait Chunk[+A]

  /**
   * An actual chunk of data.
   */
  case class Data[A](data: A) extends Chunk[A]

  /**
   * The EOF marker.
   */
  case object EOF extends Chunk[Nothing]

  /**
   * Abstraction of stream control (i.e. back-pressure handling).
   */
  trait Subscription {
    /**
     * Resume reading (i.e. resume pushing more chunks to the stream).
     */
    def requestMore()

    /**
     * Abort reading (i.e. interrupt the transmission asap).
     */
    def cancel(msg: String)
  }

  /**
   * Little helper if there is no stream control whatsoever.
   * E.g. when all the data is already fully in memory.
   */
  object NoSubscription extends Subscription {
    override def requestMore() {}

    override def cancel(msg: String) {}
  }

  /**
   * Little helper to create a /dev/null sink.
   */
  def nullStream[A]: PMSubscriber[A] = new PMSubscriber[A] {
    private var subscription: Subscription = NoSubscription

    override def onSubscribe(_subscription: Subscription) {
      subscription = _subscription
    }

    override def onNext(chunk: Chunk[A]) {
      subscription.requestMore()
    }
  }
}