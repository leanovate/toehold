package de.leanovate.akka.tcp

import org.scalatest.{Matchers, FunSpec}
import de.leanovate.akka.tcp.PMStream.{NoControl, Data, Chunk, Control}
import org.mockito.Mockito.{spy, verify, verifyZeroInteractions, verifyNoMoreInteractions}
import de.leanovate.akka.testutil.RealMockitoSugar

class PMStreamSpec extends FunSpec with Matchers with RealMockitoSugar {
  describe("PMStream") {
    describe("send chunks") {
      it("should only forward the last resume") {
        val control = mock[Control]
        val stream = spy(new NullPMStream)

        stream.send(Seq(Data("Chunk1"), Data("Chunk2"), Data("Chunk3")), control)

        val control1 = captor[Control]
        verify(stream).send(is(Data("Chunk1")), control1.capture())
        control1.getValue.resume()
        val control2 = captor[Control]
        verify(stream).send(is(Data("Chunk2")), control2.capture())
        control2.getValue.resume()
        val control3 = captor[Control]
        verify(stream).send(is(Data("Chunk3")), control3.capture())
        verifyZeroInteractions(control)
        control3.getValue.resume()
        verify(control).resume()
        verifyNoMoreInteractions(control)
      }

      it("should foward all errors") {
        val control = mock[Control]
        val stream = spy(new NullPMStream)

        stream.send(Seq(Data("Chunk1"), Data("Chunk2"), Data("Chunk3")), control)

        val control1 = captor[Control]
        verify(stream).send(is(Data("Chunk1")), control1.capture())
        control1.getValue.abort("Error1")
        val control2 = captor[Control]
        verify(stream).send(is(Data("Chunk2")), control2.capture())
        control2.getValue.abort("Error2")
        val control3 = captor[Control]
        verify(stream).send(is(Data("Chunk3")), control3.capture())
        control3.getValue.abort("Error3")
        verify(control).abort("Error1")
        verify(control).abort("Error2")
        verify(control).abort("Error3")
        verifyNoMoreInteractions(control)
      }
    }

    it("should push directly") {
      val stream = spy(new NullPMStream)

      stream.push("Chunk1", "Chunk2", "Chunk3")

      verify(stream).send(Seq(Data("Chunk1"), Data("Chunk2"), Data("Chunk3")), NoControl)
    }
  }

  class NullPMStream extends PMStream[String] {
    override def send(chunk: Chunk[String], ctrl: Control) {
    }
  }

}

