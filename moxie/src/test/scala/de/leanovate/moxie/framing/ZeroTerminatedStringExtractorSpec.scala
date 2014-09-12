/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.framing

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import akka.util.ByteString
import de.leanovate.moxie.testutil.CollectingPMSubscriber
import de.leanovate.akka.tcp.PMSubscriber.EOF

class ZeroTerminatedStringExtractorSpec extends Specification with ShouldMatchers {
  "ZeroTerminatedStringExtractor" should {
    "extract all zero terminated strings" in {
      val out = new CollectingPMSubscriber[ByteString]
      val pipe = Framing.zeroTerminatedString |> out

      pipe.push(ByteString("Hello\u0000World\u0000Nothing"))
      pipe.onNext(EOF)

      out.eof should beTrue
      out.result() shouldEqual Seq(ByteString("Hello"), ByteString("World"), ByteString("Nothing"))
    }

    "extract all zero terminstated strings from arbitrary chunks" in {
      val out = new CollectingPMSubscriber[ByteString]
      val pipe = Framing.zeroTerminatedString |> out

      pipe.push(ByteString("Hel"), ByteString("lo\u0000World\u0000Not"), ByteString("hing\u0000"), ByteString("Last"))
      pipe.onNext(EOF)

      out.eof should beTrue
      out.result() shouldEqual Seq(ByteString("Hello"), ByteString("World"), ByteString("Nothing"), ByteString("Last"))
    }
  }
}
