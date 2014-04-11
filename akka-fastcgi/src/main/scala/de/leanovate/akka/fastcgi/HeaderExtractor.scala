/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import de.leanovate.akka.iteratee.tcp.PMStream
import akka.util.ByteString

class HeaderExtractor(headers: (Int, String, Seq[(String, String)]) => Unit, body: PMStream[ByteString])
  extends PMStream[ByteString] {

  class ExtractHeadersState extends PMStream[ByteString] {
    var buffer = ByteString.empty

    val lines = Seq.newBuilder[(String, String)]

    var statusCode = 200

    var statusLine = "OK"

    override def sendChunk(data: ByteString, ctrl: PMStream.Control) = {

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
          state = body
          headers(statusCode, statusLine, lines.result())
          if (!buffer.isEmpty) {
            body.sendChunk(buffer, ctrl)
            buffer = ByteString.empty
          } else {
            ctrl.resume()
          }
          done = true
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
      if (!done) {
        ctrl.resume()
      }
    }

    override def sendEOF() = {

      headers(statusCode, statusLine, lines.result())
      body.sendEOF()
    }
  }

  var state: PMStream[ByteString] = new ExtractHeadersState

  override def sendChunk(data: ByteString, ctrl: PMStream.Control) = state.sendChunk(data, ctrl)

  override def sendEOF() = state.sendEOF()
}
