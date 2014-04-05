package de.leanovate.akka.fastcgi.records

import play.api.libs.iteratee.{Enumeratee, Enumerator, Input}
import akka.util.ByteString
import scala.concurrent.ExecutionContext

class BytesToFCGIRecords extends (Input[ByteString] => Enumerator[FCGIRecord]) {
  private var buffer = ByteString.empty

  override def apply(in: Input[ByteString]) = in match {
    case Input.El(chunk) =>
      buffer ++= chunk
      val records = Seq.newBuilder[FCGIRecord]
      var extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
      while (extracted._1.isDefined) {
        records += extracted._1.get
        extracted = FCGIRecord.decode(buffer)
        buffer = extracted._2
      }
      Enumerator(records.result(): _*)
    case Input.EOF if buffer.isEmpty =>
      Enumerator.eof
    case Input.EOF =>
      val records = Seq.newBuilder[FCGIRecord]
      var extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
      while (extracted._1.isDefined) {
        records += extracted._1.get
        extracted = FCGIRecord.decode(buffer)
        buffer = extracted._2
      }
      Enumerator(records.result(): _*) >>> Enumerator.eof
    case Input.Empty =>
      Enumerator.empty
  }
}
