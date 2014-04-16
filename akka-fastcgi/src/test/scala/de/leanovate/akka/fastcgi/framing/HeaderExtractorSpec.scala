/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.framing

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import de.leanovate.akka.testutil.CollectingPMStream
import akka.util.ByteString
import org.specs2.mock.Mockito
import de.leanovate.akka.tcp.PMPipe
import de.leanovate.akka.tcp.PMStream.{EOF, NoControl}

class HeaderExtractorSpec extends Specification with ShouldMatchers with Mockito {
  "HeaderExtractor" should {
    "not forward before the first empty line" in {
      val out = new CollectingPMStream[ByteString]
      val headersCallback = mock[(Int, String, Seq[(String, String)]) => Unit]
      val pipe = PMPipe.flatMapChunk(new HeaderExtractor(headersCallback)) |> out

      pipe.push(ByteString("Status: 400 Badbad\r\n"), ByteString("Content-Type: text/plain\r\n"))

      there was noCallsTo(headersCallback)
      out.eof should beFalse
      out.result() should beEmpty

      pipe.push(ByteString("\r\n12345"))
      there was one(headersCallback).apply(400, "Badbad", Seq("Content-Type" -> "text/plain"))
      out.eof should beFalse

      pipe.send(EOF, NoControl)

      out.eof should beTrue
      out.result() shouldEqual Seq(ByteString("12345"))
    }

    "report all parsed headers on eof" in {
      val out = new CollectingPMStream[ByteString]
      val headersCallback = mock[(Int, String, Seq[(String, String)]) => Unit]
      val pipe = PMPipe.flatMapChunk(new HeaderExtractor(headersCallback)) |> out

      pipe.push(ByteString("Status: 400 Badbad\r\n"), ByteString("Content-Type: text/plain\r\nBlablue"), ByteString("Blub"))

      there was noCallsTo(headersCallback)

      pipe.send(EOF, NoControl)

      there was one(headersCallback).apply(400, "Badbad", Seq("Content-Type" -> "text/plain"))
      out.eof should beTrue
      out.result() should beEmpty
    }

    "be able to parse lines broken in different chunks" in {
      val out = new CollectingPMStream[ByteString]
      val headersCallback = mock[(Int, String, Seq[(String, String)]) => Unit]
      val pipe = PMPipe.flatMapChunk(new HeaderExtractor(headersCallback)) |> out

      pipe.push(ByteString("Status: 404 Notfound\r\nContent-Type: text/plain\nETag"), ByteString(": 1234567\r\nLa"),
                 ByteString("st-Modified: 7654\r\n\r"), ByteString("\n987654321"))
      pipe.send(EOF, NoControl)

      there was one(headersCallback).apply(404, "Notfound", Seq("Content-Type" -> "text/plain", "ETag" -> "1234567", "Last-Modified" -> "7654"))
      out.eof should beTrue
      out.result() shouldEqual Seq(ByteString("987654321"))
    }

    "parse statusCode without status text" in {
      val out = new CollectingPMStream[ByteString]
      val headersCallback = mock[(Int, String, Seq[(String, String)]) => Unit]
      val pipe = PMPipe.flatMapChunk(new HeaderExtractor(headersCallback)) |> out

      pipe.push(ByteString("Status: 500\r\n"), ByteString("Content-Type: text/plain\r\n\r\n"))

      there was one(headersCallback).apply(500, "OK", Seq("Content-Type" -> "text/plain"))
      out.eof should beFalse
      out.result() should beEmpty
    }
  }
}
