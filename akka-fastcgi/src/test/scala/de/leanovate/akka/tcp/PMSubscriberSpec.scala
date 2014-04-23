/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMSubscriber.{Data, Chunk, Subscription}
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito

class PMSubscriberSpec extends Specification with ShouldMatchers with Mockito {
  "PMStream" should {
    "should push directly" in {
      val stream = spy(new NullPMSubscriber)

      stream.push("Chunk1", "Chunk2", "Chunk3")

      there was one(stream).onNext(Data("Chunk1"))
      there was one(stream).onNext(Data("Chunk2"))
      there was one(stream).onNext(Data("Chunk3"))
    }
  }

  class NullPMSubscriber extends PMSubscriber[String] {
    override def onSubscribe(subscription: Subscription) {

    }

    override def onNext(chunk: Chunk[String]) {
    }
  }

}

