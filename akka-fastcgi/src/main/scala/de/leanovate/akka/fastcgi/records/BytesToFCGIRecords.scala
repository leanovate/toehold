package de.leanovate.akka.fastcgi.records

import play.api.libs.iteratee.{Enumeratee, Enumerator, Input}
import akka.util.ByteString
import scala.concurrent.ExecutionContext
import de.leanovate.akka.iteratee.tcp.FeedSink

class BytesToFCGIRecords(target: FeedSink[FCGIRecord]) extends FeedSink[ByteString] {
  private var buffer = ByteString.empty

  override def feedChunk(data: ByteString) = {
    buffer ++= data
    var extracted = FCGIRecord.decode(buffer)
    buffer = extracted._2
    while (extracted._1.isDefined) {
      target.feedChunk(extracted._1.get)
      extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
    }
  }

  override def feedEOF() = {
    var extracted = FCGIRecord.decode(buffer)
    buffer = extracted._2
    while (extracted._1.isDefined) {
      target.feedChunk(extracted._1.get)
      extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
    }
    target.feedEOF()
  }
}
