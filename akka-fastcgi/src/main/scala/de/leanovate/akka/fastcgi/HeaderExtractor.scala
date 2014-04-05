package de.leanovate.akka.fastcgi

import de.leanovate.akka.iteratee.tcp.FeedSink
import akka.util.ByteString
import play.api.libs.iteratee.{Cont, Input, Done}

class HeaderExtractor(headers: Seq[(String, String)] => Unit, body: FeedSink[ByteString]) extends FeedSink[ByteString] {
  var buffer = ByteString.empty

  val lines = Seq.newBuilder[(String, String)]

  var end = false

  override def feedChunk(data: ByteString) = {

    if (end) {
      body.feedChunk(data)
    } else {
      buffer ++= data
      var idx = buffer.indexOf('\n')

      while (!end && idx >= 0) {
        val line = if (idx > 0 && buffer(idx - 1) == '\r') {
          buffer.take(idx - 1)
        } else {
          buffer.take(idx)
        }
        buffer = buffer.drop(idx + 1)
        idx = buffer.indexOf('\n')
        if (line.isEmpty) {
          end = true
          headers(lines.result())
          if (!buffer.isEmpty) {
            body.feedChunk(buffer)
            buffer = ByteString.empty
          }
        } else {
          val delimIdx = line.indexOf(':')
          if (delimIdx >= 0) {
            lines += line.take(delimIdx).utf8String -> line.drop(delimIdx + 1).utf8String.trim
          }
        }
      }
    }
  }

  override def feedEOF() = {

    if (!end) {
      headers(lines.result())
    }
    body.feedEOF()
  }
}
