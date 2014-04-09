package de.leanovate.akka.fastcgi.records

import de.leanovate.akka.iteratee.tcp.DataSink
import akka.util.ByteString

class FilterStdOut(stderr: ByteString => Unit, target: DataSink[ByteString]) extends DataSink[FCGIRecord] {
  var done = false

  override def sendChunk(data: FCGIRecord) = if (!done) {
    data match {
      case FCGIStdOut(_, content) =>
        target.sendChunk(content)
      case FCGIStdErr(_, content) =>
        stderr(content)
      case _: FCGIEndRequest =>
        sendEOF()
    }
  }

  override def sendEOF() = if (!done) {
    done = true
    target.sendEOF()
  }
}
