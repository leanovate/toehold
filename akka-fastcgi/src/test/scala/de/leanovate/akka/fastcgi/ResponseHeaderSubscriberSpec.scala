/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import akka.util.ByteString
import de.leanovate.akka.testutil.CollectingPMSubscriber
import org.specs2.specification.Scope
import de.leanovate.akka.tcp.PMSubscriber.EOF

class ResponseHeaderSubscriberSpec extends Specification with ShouldMatchers {
  "ResponseHeaderSubscriber" should {
    "not forward before the first empty line" in new WithMockResponseHeaderSubscriber {
      responseHeaderSubscriber.push(ByteString("Status: 400 Badbad\r\n"), ByteString("Content-Type: text/plain\r\n"))

      statusCode should beNone
      out.eof should beFalse
      out.result() should beEmpty

      responseHeaderSubscriber.push(ByteString("\r\n12345"))
      statusCode should beSome(400)
      statusLine should beSome("Badbad")
      headers should_== Some(Seq("Content-Type" -> "text/plain"))
      out.eof should beFalse

      responseHeaderSubscriber.onNext(EOF)

      out.eof should beTrue
      out.result() shouldEqual Seq(ByteString("12345"))
    }

    "report all parsed headers on eof" in new WithMockResponseHeaderSubscriber {
      responseHeaderSubscriber.push(ByteString("Status: 400 Badbad\r\n"), ByteString("Content-Type: text/plain\r\nBlablue"), ByteString("Blub"))

      statusCode should beNone

      responseHeaderSubscriber.onNext(EOF)

      statusCode should beSome(400)
      statusLine should beSome("Badbad")
      headers should_== Some(Seq("Content-Type" -> "text/plain"))
      out.eof should beTrue
      out.result() should beEmpty
    }

    "be able to parse lines broken in different chunks" in new WithMockResponseHeaderSubscriber {
      responseHeaderSubscriber.push(ByteString("Status: 404 Notfound\r\nContent-Type: text/plain\nETag"), ByteString(": 1234567\r\nLa"),
        ByteString("st-Modified: 7654\r\n\r"), ByteString("\n987654321"))
      responseHeaderSubscriber.onNext(EOF)

      statusCode should beSome(404)
      statusLine should beSome("Notfound")
      headers should_== Some(Seq("Content-Type" -> "text/plain", "ETag" -> "1234567", "Last-Modified" -> "7654"))
      out.eof should beTrue
      out.result() shouldEqual Seq(ByteString("987654321"))
    }

    "parse statusCode without status text" in new WithMockResponseHeaderSubscriber {
      responseHeaderSubscriber.push(ByteString("Status: 500\r\n"), ByteString("Content-Type: text/plain\r\n\r\n"))

      statusCode should beSome(500)
      statusLine should beSome("OK")
      headers should_== Some(Seq("Content-Type" -> "text/plain"))
      out.eof should beFalse
      out.result() should beEmpty
    }
  }

  trait WithMockResponseHeaderSubscriber extends Scope {
    val out = new CollectingPMSubscriber[ByteString]
    var statusCode: Option[Int] = None
    var statusLine: Option[String] = None
    var headers: Option[Seq[(String, String)]] = None

    val responseHeaderSubscriber = new ResponseHeaderSubscriber {
      override def onHeader(_statusCode: Int, _statusLine: String, _headers: Seq[(String, String)]) = {
        statusCode = Some(_statusCode)
        statusLine = Some(_statusLine)
        headers = Some(_headers)
        out
      }
    }
  }
}