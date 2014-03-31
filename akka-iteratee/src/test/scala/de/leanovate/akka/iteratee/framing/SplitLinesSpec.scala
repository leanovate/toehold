package de.leanovate.akka.iteratee.framing

import org.scalatest.{FunSpec, Matchers}
import scala.concurrent.{Await, ExecutionContext}
import play.api.libs.iteratee.{Iteratee, Enumerator}
import akka.util.ByteString
import scala.concurrent.duration._

class SplitLinesSpec extends FunSpec with Matchers {
  implicit val ec = ExecutionContext.global

  describe("Split lines enumeratee") {
    it("Should split lines of arbitrary chunked input") {
      val in = Enumerator(
                           b("Lin"),
                           b("e1\nLine2\nL"),
                           b("ine3\n"),
                           b("Line4\n")
                         )
      val out = Iteratee.getChunks[ByteString]
      val futureResult = in |>>> Framing.splitLines &>> out
      val result = Await.result(futureResult, 5 seconds)

      result should be(b("Line1\n") :: b("Line2\n") :: b("Line3\n") :: b("Line4\n") :: Nil)
    }

    it("Should not drop the last line") {
      val in = Enumerator(
                           b("Lin"),
                           b("e1\nLine2\nL"),
                           b("ine3\n"),
                           b("Line4\nLine5")
                         )

      val out = Iteratee.getChunks[ByteString]
      val futureResult = in |>>> Framing.splitLines &>> out
      val result = Await.result(futureResult, 5 seconds)

      result should be(b("Line1\n") :: b("Line2\n") :: b("Line3\n") :: b("Line4\n") :: b("Line5") :: Nil)
    }
  }

  private def b(str: String) = ByteString(str)
}
