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
      var extracted = FCGIRecord.extract(buffer)
      while (extracted.isDefined) {
        records += extracted.get._1
        buffer = extracted.get._2
        extracted = FCGIRecord.extract(buffer)
      }
      Enumerator(records.result(): _*)
    case Input.EOF if buffer.isEmpty =>
      Enumerator.eof
    case Input.EOF =>
      val records = Seq.newBuilder[FCGIRecord]
      var extracted = FCGIRecord.extract(buffer)
      while (extracted.isDefined) {
        records += extracted.get._1
        buffer = extracted.get._2
        extracted = FCGIRecord.extract(buffer)
      }
      Enumerator(records.result(): _*) >>> Enumerator.eof
    case Input.Empty =>
      Enumerator.empty
  }
}

object BytesToFCGIRecords {

  def enumeratee(implicit ctx: ExecutionContext) = Enumeratee.mapInputFlatten(new BytesToFCGIRecords)
}