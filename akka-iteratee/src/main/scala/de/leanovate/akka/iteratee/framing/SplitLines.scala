package de.leanovate.akka.iteratee.framing

import play.api.libs.iteratee.{Enumerator, Input}
import akka.util.ByteString

class SplitLines extends (Input[ByteString] => Enumerator[ByteString]) {
  private var buffer = ByteString.empty

  override def apply(in: Input[ByteString]): Enumerator[ByteString] = in match {
    case Input.El(chunk) =>
      buffer ++= chunk
      val lines = Seq.newBuilder[ByteString]
      var idx = buffer.indexOf('\n')
      while (idx >= 0) {
        lines += buffer.take(idx + 1)
        buffer = buffer.drop(idx + 1)
        idx = buffer.indexOf('\n')
      }
      Enumerator(lines.result(): _*)
    case Input.EOF if buffer.isEmpty =>
      Enumerator.eof
    case Input.EOF =>
      Enumerator(buffer) >>> Enumerator.eof
    case Input.Empty =>
      Enumerator.empty
  }
}
