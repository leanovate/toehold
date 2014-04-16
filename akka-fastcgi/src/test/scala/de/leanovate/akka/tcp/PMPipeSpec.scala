package de.leanovate.akka.tcp

import de.leanovate.akka.testutil.CollectingPMStream
import de.leanovate.akka.tcp.PMStream.{Data, NoControl, EOF}
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito

class PMPipeSpec extends Specification with ShouldMatchers with Mockito {
  "PMPipe" should {
    "support simple data mapping" in {
      val out = new CollectingPMStream[Int]
      val pipe = PMPipe.map[String, Int](_.toInt) |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should beTrue
      out.result() shouldEqual Seq(1, 2, 3)
    }

    "support chunk mapping" in {
      val out = new CollectingPMStream[Int]
      val pipe = PMPipe.mapChunk[String, Int] {
        case Data(str) => Data(str.toInt)
        case EOF => Data(0)
      } |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should beFalse
      out.result() shouldEqual Seq(1, 2, 3, 0)
    }

    "support flatMap on chunk" in {
      val out = new CollectingPMStream[Int]
      val pipe = PMPipe.flatMapChunk[String, Int] {
        case Data(str) => Range(0, str.toInt + 1).map(Data(_))
        case EOF => Seq(Data(0))
      } |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should beFalse
      out.result() shouldEqual Seq(0, 1, 0, 1, 2, 0, 1, 2, 3, 0)
    }

    "should be able to concat associatively" in {
      val concatPipe = PMPipe.map[String, Int](_.toInt) |> PMPipe.flatMapChunk[Int, Int] {
        case Data(idx) => Range(0, idx + 1).map(Data(_))
        case EOF => Seq(Data(0))
      }
      val out = new CollectingPMStream[Int]
      val pipe = concatPipe |> out

      pipe.push("1", "2", "3")
      pipe.send(EOF, NoControl)

      out.eof should beFalse
      out.result() shouldEqual Seq(0, 1, 0, 1, 2, 0, 1, 2, 3, 0)
    }
  }
}
