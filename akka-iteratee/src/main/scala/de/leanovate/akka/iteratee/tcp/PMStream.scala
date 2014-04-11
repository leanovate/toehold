package de.leanovate.akka.iteratee.tcp

import java.util.concurrent.atomic.AtomicInteger

/**
 * Poor man's (reactive) streaming.
 *
 * This is most certainly not the best api for this kind of task, it is just supposed to avoid Future-cascades.
 * Will most likely be removed once akka has its own reactive stream (which might happen with 2.4).
 */
trait PMStream[A] {
  def sendChunk(data: A, resume: () => Unit)

  def sendEOF()
}

object PMStream {

  class CountdownResumer(resume: () => Unit) extends (() => Unit) {
    private val counter = new AtomicInteger(1)

    def increment() = counter.incrementAndGet()

    def apply() {

      if (counter.decrementAndGet() <= 0) {
        resume()
      }
    }
  }

}