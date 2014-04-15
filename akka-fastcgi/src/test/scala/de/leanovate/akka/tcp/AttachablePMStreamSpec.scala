package de.leanovate.akka.tcp

import org.scalatest.{Matchers, FunSpec}
import de.leanovate.akka.testutil.{CollectingPMStream, RealMockitoSugar}
import de.leanovate.akka.tcp.PMStream.{Data, Control}
import org.mockito.Mockito.{verify, verifyZeroInteractions, times}

class AttachablePMStreamSpec extends FunSpec with Matchers with RealMockitoSugar {
  describe("AttachablePMStream") {
    it("should buffer data in unattached state") {
      val attachable = new AttachablePMStream[String]
      val ctrl = mock[Control]

      attachable.send(Data("1"), ctrl)
      attachable.send(Data("2"), ctrl)
      attachable.send(Data("3"), ctrl)
      verifyZeroInteractions(ctrl)

      val out = new CollectingPMStream[String](resuming = true)

      attachable.attach(out)

      verify(ctrl, times(1)).resume()

      out.eof should be(false)
      out.result() should be(Seq("1", "2", "3"))
    }

    it("should pass through after attached") {
      val attachable = new AttachablePMStream[String]
      val ctrl = mock[Control]
      val out = new CollectingPMStream[String](resuming = true)

      attachable.attach(out)

      attachable.send(Data("1"), ctrl)
      attachable.send(Data("2"), ctrl)
      attachable.send(Data("3"), ctrl)

      verify(ctrl, times(3)).resume()

      out.eof should be(false)
      out.result() should be(Seq("1", "2", "3"))
    }
  }
}
