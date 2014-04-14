/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.framing

import org.scalatest.{Matchers, FunSpec}
import de.leanovate.akka.testutil.CollectingPMStream
import akka.util.ByteString
import de.leanovate.akka.fastcgi.records.{FCGIEndRequest, FCGIStdErr, FCGIStdOut, FCGIRecord}
import de.leanovate.akka.tcp.PMStream.{EOF, NoControl, Data}

class FilterStdOutSpec extends FunSpec with Matchers {
  describe("FilterStdOut") {
    it("should only only pass FCGISTdOut records") {
      val stderrs = Seq.newBuilder[ByteString]
      val out = new CollectingPMStream[ByteString]
      val pipe = Framing.filterStdOut(stderr => stderrs += stderr) |> out

      pipe.push(FCGIStdOut(1, ByteString("Hello")), FCGIStdErr(1, ByteString("something")),
                 FCGIStdOut(1, ByteString("World")), FCGIEndRequest(1))

      out.eof should be(true)
      stderrs.result() should be(Seq(ByteString("something")))
      out.result() should be(Seq(ByteString("Hello"), ByteString("World")))
    }

    it("should honour eof") {
      val stderrs = Seq.newBuilder[ByteString]
      val out = new CollectingPMStream[ByteString]
      val pipe = Framing.filterStdOut(stderr => stderrs += stderr) |> out

      pipe.send(Data(FCGIStdOut(1, ByteString("Hello"))), NoControl)
      pipe.send(Data(FCGIStdErr(1, ByteString("something"))), NoControl)
      pipe.send(EOF, NoControl)
      pipe.send(Data(FCGIStdOut(1, ByteString("World"))), NoControl)

      out.eof should be(true)
      stderrs.result() should be(Seq(ByteString("something")))
      out.result() should be(Seq(ByteString("Hello")))
    }
  }
}
