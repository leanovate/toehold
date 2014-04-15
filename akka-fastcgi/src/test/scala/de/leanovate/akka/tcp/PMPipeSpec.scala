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

    it("should support flatMap on chunk") {
      val out = new CollectingPMStream[Int]
      val pipe = PMPipe.flatMapChunk[String, Int] {
        case Data(str) => Range(0, str.toInt + 1).map(Data(_))
        case EOF => Seq(Data(0))
      } |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should be(false)
      out.result() should be(Seq(0, 1, 0, 1, 2, 0, 1, 2, 3, 0))
    }

    it("should be able to concat associatively") {
      val concatPipe = PMPipe.map[String, Int](_.toInt) |> PMPipe.flatMapChunk[Int, Int] {
        case Data(idx) => Range(0, idx + 1).map(Data(_))
        case EOF => Seq(Data(0))
      }
      val out = new CollectingPMStream[Int]
      val pipe = concatPipe |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should be(false)
      out.result() should be(Seq(0, 1, 0, 1, 2, 0, 1, 2, 3, 0))
    }
  }
}
