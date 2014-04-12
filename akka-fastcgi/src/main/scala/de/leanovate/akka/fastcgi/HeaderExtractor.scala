/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import de.leanovate.akka.iteratee.tcp.PMStream
import akka.util.ByteString
import de.leanovate.akka.iteratee.tcp.PMStream.{EOF, Data, Control, Chunk}

class HeaderExtractor(headers: (Int, String, Seq[(String, String)]) => Unit, body: PMStream[ByteString])
  extends PMStream[ByteString] {

  class ExtractHeadersState extends PMStream[ByteString] {
    var buffer = ByteString.empty

    val lines = Seq.newBuilder[(String, String)]

    var statusCode = 200

    var statusLine = "OK"

    override def send(chunk: Chunk[ByteString], ctrl: Control) = {

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
              state = body
              headers(statusCode, statusLine, lines.result())
              if (!buffer.isEmpty) {
                body.send(Data(buffer), ctrl)
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
        case EOF =>
          headers(statusCode, statusLine, lines.result())
          body.send(EOF, ctrl)
      }
    }
  }

  var state: PMStream[ByteString] = new ExtractHeadersState

  override def send(chunk: Chunk[ByteString], ctrl: Control) = state.send(chunk, ctrl)
}
