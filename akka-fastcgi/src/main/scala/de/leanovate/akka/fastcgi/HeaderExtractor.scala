/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import de.leanovate.akka.iteratee.tcp.DataSink
import akka.util.ByteString

class HeaderExtractor(headers: (Int, String, Seq[(String, String)]) => Unit, body: DataSink[ByteString]) extends DataSink[ByteString] {

  sealed trait State {
    def sendChunk(data: ByteString)

    def sendEOF()
  }

  class ExtractHeadersState extends State {
    var buffer = ByteString.empty

    val lines = Seq.newBuilder[(String, String)]

    var statusCode = 200

    var statusLine = "OK"

    override def sendChunk(data: ByteString) = {

      buffer ++= data
      var idx = buffer.indexOf('\n')

      while (idx >= 0) {
        val line = if (idx > 0 && buffer(idx - 1) == '\r') {
          buffer.take(idx - 1)
        } else {
          buffer.take(idx)
        }
        buffer = buffer.drop(idx + 1)
        idx = buffer.indexOf('\n')
        if (line.isEmpty) {
          state = new PassThruState()
          headers(statusCode, statusLine, lines.result())
          if (!buffer.isEmpty) {
            body.sendChunk(buffer)
            buffer = ByteString.empty
          }
          idx = -1
        } else {
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
    }

    override def sendEOF() = {

      headers(statusCode, statusLine, lines.result())
      body.sendEOF()
    }
  }

  class PassThruState extends State {
    override def sendChunk(data: ByteString) = body.sendChunk(data)

    override def sendEOF() = body.sendEOF()
  }

  var state: State = new ExtractHeadersState

  override def sendChunk(data: ByteString) = state.sendChunk(data)

  override def sendEOF() = state.sendEOF()
}
