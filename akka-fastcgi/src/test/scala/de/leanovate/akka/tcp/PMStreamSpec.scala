package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMStream.{NoControl, Data, Chunk, Control}
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito

class PMStreamSpec extends Specification with ShouldMatchers with Mockito {
  "PMStream" should {
    "only forward the last resume on send chunks" in {
      val control = mock[Control]
      val stream = spy(new NullPMStream)

      stream.sendSeq(Seq(Data("Chunk1"), Data("Chunk2"), Data("Chunk3")), control)

      val control1 = capture[Control]
      there was one(stream).send(typedEqualTo(Data("Chunk1")), control1.capture)
      control1.value.resume()
      val control2 = capture[Control]
      there was one(stream).send(typedEqualTo(Data("Chunk2")), control2.capture)
      control2.value.resume()
      val control3 = capture[Control]
      there was one(stream).send(typedEqualTo(Data("Chunk3")), control3.capture)
      there was noCallsTo(control)
      control3.value.resume()
      there was one(control).resume()
      there was noMoreCallsTo(control)
    }

    "forward all errors on send chunks" in {
      val control = mock[Control]
      val stream = spy(new NullPMStream)

      stream.sendSeq(Seq(Data("Chunk1"), Data("Chunk2"), Data("Chunk3")), control)

      val control1 = capture[Control]
      there was one(stream).send(typedEqualTo(Data("Chunk1")), control1.capture)
      control1.value.abort("Error1")
      val control2 = capture[Control]
      there was one(stream).send(typedEqualTo(Data("Chunk2")), control2.capture)
      control2.value.abort("Error2")
      val control3 = capture[Control]
      there was one(stream).send(typedEqualTo(Data("Chunk3")), control3.capture)
      control3.value.abort("Error3")
      there was one(control).abort("Error1")
      there was one(control).abort("Error2")
      there was one(control).abort("Error3")
      there was noMoreCallsTo(control)
    }

    "should push directly" in {
      val stream = spy(new NullPMStream)

      stream.push("Chunk1", "Chunk2", "Chunk3")

      there was one(stream).sendSeq(Seq(Data("Chunk1"), Data("Chunk2"), Data("Chunk3")), NoControl)
      true
    }
  }

  class NullPMStream extends PMStream[String] {
    override def send(chunk: Chunk[String], ctrl: Control) {
    }
  }

}

