package de.leanovate.akka.fastcgi.records

import de.leanovate.akka.iteratee.tcp.FeedSink
import akka.util.ByteString

class FilterStdOut(stderr: ByteString => Unit, target: FeedSink[ByteString]) extends FeedSink[FCGIRecord] {
  var done = false

  override def feedChunk(data: FCGIRecord) = if (!done) {
    data match {
      case FCGIStdOut(_, content) =>
        target.feedChunk(content)
      case FCGIStdErr(_, content) =>
        stderr(content)
      case _: FCGIEndRequest =>
        feedEOF()
    }
  }

  override def feedEOF() = if (!done) {
    done = true
    target.feedEOF()
  }
}
