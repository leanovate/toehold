/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import de.leanovate.akka.testutil.CollectingPMStream
import de.leanovate.akka.tcp.PMStream.{Data, Control}
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito

class AttachablePMStreamSpec extends Specification with ShouldMatchers with Mockito {
  "AttachablePMStream" should {
    "buffer data in unattached state" in {
      val attachable = new AttachablePMStream[String]
      val ctrl = mock[Control]

      attachable.send(Data("1"), ctrl)
      attachable.send(Data("2"), ctrl)
      attachable.send(Data("3"), ctrl)
      there was noCallsTo(ctrl)

      val out = new CollectingPMStream[String](resuming = true)

      attachable.attach(out)

      there was one(ctrl).resume()

      out.eof should beFalse
      out.result() shouldEqual Seq("1", "2", "3")
    }

    "should pass through after attached" in {
      val attachable = new AttachablePMStream[String]
      val ctrl = mock[Control]
      val out = new CollectingPMStream[String](resuming = true)

      attachable.attach(out)

      attachable.send(Data("1"), ctrl)
      attachable.send(Data("2"), ctrl)
      attachable.send(Data("3"), ctrl)

      there was three(ctrl).resume()

      out.eof should beFalse
      out.result() shouldEqual Seq("1", "2", "3")
    }
  }
}
