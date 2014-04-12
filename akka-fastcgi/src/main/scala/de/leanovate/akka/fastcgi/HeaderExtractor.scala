/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import de.leanovate.akka.tcp.PMStream
import akka.util.ByteString
import de.leanovate.akka.tcp.PMStream.{EOF, Data, Control, Chunk}

class HeaderExtractor(headers: (Int, String, Seq[(String, String)]) => Unit)
  extends (Chunk[ByteString] => Seq[Chunk[ByteString]]) {

  class ExtractHeadersState extends (Chunk[ByteString] => Seq[Chunk[ByteString]]) {
    var buffer = ByteString.empty

    val lines = Seq.newBuilder[(String, String)]

    var statusCode = 200

    var statusLine = "OK"

    override def apply(chunk: Chunk[ByteString]): Seq[Chunk[ByteString]] = {

      chunk match {
        case Data(data) =>
          buffer ++= data
          var idx = buffer.indexOf('\n')
          var done = false

          while (idx >= 0 && !done) {
            val line = if (idx > 0 && buffer(idx - 1) == '\r') {
              buffer.take(idx - 1)
            } else {
              buffer.take(idx)
            }
            buffer = buffer.drop(idx + 1)
            idx = buffer.indexOf('\n')
            if (line.isEmpty) {
              state = new PassThruState
              headers(statusCode, statusLine, lines.result())
              done = true
            } else {
              parseLine(line)
            }
          }
          if (done && !buffer.isEmpty) {
            Seq(Data(buffer))
          } else {
            Seq.empty
          }
        case EOF =>
          headers(statusCode, statusLine, lines.result())
          Seq(EOF)
      }
    }

    private def parseLine(line: ByteString) = {

      val delimIdx = line.indexOf(':')
      if (delimIdx >= 0) {
        val name = line.take(delimIdx).utf8String
        val value = line.drop(delimIdx + 1).utf8String.trim

        if (name.equalsIgnoreCase("status")) {
          val splitIdx = value.indexOf(' ')
          if (splitIdx < 0) {
            statusCode = value.toInt
          } else {
            statusCode = value.substring(0, splitIdx).trim.toInt
            statusLine = value.substring(splitIdx + 1).trim
          }
        } else {
          lines += name -> value
        }
      }
    }
  }

  class PassThruState extends (Chunk[ByteString] => Seq[Chunk[ByteString]]) {
    override def apply(chunk: Chunk[ByteString]) = Seq(chunk)
  }

  var state: (Chunk[ByteString] => Seq[Chunk[ByteString]]) = new ExtractHeadersState

  override def apply(chunk: Chunk[ByteString]) = state(chunk)
}
