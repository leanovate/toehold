/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.records

import de.leanovate.akka.iteratee.tcp.PMStream
import akka.util.ByteString

class FilterStdOut(stderr: ByteString => Unit, target: PMStream[ByteString]) extends PMStream[FCGIRecord] {
  var done = false

  override def sendChunk(data: FCGIRecord, resume: () => Unit) = if (!done) {
    data match {
      case FCGIStdOut(_, content) =>
        target.sendChunk(content, resume)
      case FCGIStdErr(_, content) =>
        stderr(content)
        resume()
      case _: FCGIEndRequest =>
        sendEOF()
        resume()
    }
  }

  override def sendEOF() = if (!done) {
    done = true
    target.sendEOF()
  }
}
