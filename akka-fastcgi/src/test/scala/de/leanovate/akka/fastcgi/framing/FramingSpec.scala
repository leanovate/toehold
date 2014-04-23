/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.framing

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import de.leanovate.akka.testutil.CollectingPMConsumer
import akka.util.ByteString
import de.leanovate.akka.fastcgi.records.{FCGIStdin, FCGIRecord}
import de.leanovate.akka.tcp.PMConsumer.{EOF, Data, NoSubscription}

class FramingSpec extends Specification with ShouldMatchers {
  "toFCGIStdin" should {
    "encode all input to fcgi stdin records" in {
      val out = new CollectingPMConsumer[FCGIRecord]
      val pipe = Framing.toFCGIStdin(1) |> out

      pipe.push(ByteString("Hello"), ByteString("World"))

      out.eof should beFalse
      out.result() shouldEqual Seq(FCGIStdin(1, ByteString("Hello")), FCGIStdin(1, ByteString("World")))
    }

    "encode eof to empty fcgi stdin record" in {
      val out = new CollectingPMConsumer[FCGIRecord]
      val pipe = Framing.toFCGIStdin(1) |> out

      pipe.onNext(Data(ByteString("Hello")))
      pipe.onNext(EOF)

      out.eof should beFalse
      out.result() shouldEqual Seq(FCGIStdin(1, ByteString("Hello")), FCGIStdin(1, ByteString.empty))
    }
  }

  "byteArrayToByteString" should {
    "convert byte arrays" in {
      val out = new CollectingPMConsumer[ByteString]
      val pipe = Framing.byteArrayToByteString |> out

      pipe.push(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6))

      out.eof should beFalse
      out.result() shouldEqual Seq(ByteString(1, 2, 3), ByteString(4, 5, 6))
    }
  }
}
