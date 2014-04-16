package de.leanovate.akka.fastcgi.framing

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import de.leanovate.akka.testutil.CollectingPMStream
import akka.util.ByteString
import de.leanovate.akka.fastcgi.records.{FCGIStdin, FCGIRecord}
import de.leanovate.akka.tcp.PMStream.{EOF, Data, NoControl}

class FramingSpec extends Specification with ShouldMatchers {
  "toFCGIStdin" should {
    "encode all input to fcgi stdin records" in {
      val out = new CollectingPMStream[FCGIRecord]
      val pipe = Framing.toFCGIStdin(1) |> out

      pipe.push(ByteString("Hello"), ByteString("World"))

      out.eof should beFalse
      out.result() shouldEqual Seq(FCGIStdin(1, ByteString("Hello")), FCGIStdin(1, ByteString("World")))
    }

    "encode eof to empty fcgi stdin record" in {
      val out = new CollectingPMStream[FCGIRecord]
      val pipe = Framing.toFCGIStdin(1) |> out

      pipe.send(Data(ByteString("Hello")), NoControl)
      pipe.send(EOF, NoControl)

      out.eof should beFalse
      out.result() shouldEqual Seq(FCGIStdin(1, ByteString("Hello")), FCGIStdin(1, ByteString.empty))
    }
  }

  "byteArrayToByteString" should {
    "convert byte arrays" in {
      val out = new CollectingPMStream[ByteString]
      val pipe = Framing.byteArrayToByteString |> out

      pipe.push(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6))

      out.eof should beFalse
      out.result() shouldEqual Seq(ByteString(1, 2, 3), ByteString(4, 5, 6))
    }
  }
}
