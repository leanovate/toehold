package de.leanovate.akka.tcp

import org.scalatest.{Matchers, FunSpec}
import de.leanovate.akka.testutil.{CollectingPMStream, RealMockitoSugar}
import akka.util.ByteString
import de.leanovate.akka.tcp.PMStream.{Data, NoControl, EOF}

class PMPipeSpec extends FunSpec with Matchers with RealMockitoSugar {
  describe("PMPipe") {
    it("should support simple data mapping") {
      val out = new CollectingPMStream[Int]
      val pipe = PMPipe.map[String, Int](_.toInt) |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should be(true)
      out.result() should be(Seq(1, 2, 3))
    }

    it("should support chunk mapping") {
      val out = new CollectingPMStream[Int]
      val pipe = PMPipe.mapChunk[String, Int] {
        case Data(str) => Data(str.toInt)
        case EOF => Data(0)
      } |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should be(false)
      out.result() should be(Seq(1, 2, 3, 0))
    }
  }
}
