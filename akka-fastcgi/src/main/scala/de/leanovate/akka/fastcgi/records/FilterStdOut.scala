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

  override def sendChunk(data: FCGIRecord, ctrl: PMStream.Control) = if (!done) {
    data match {
      case FCGIStdOut(_, content) =>
        target.sendChunk(content, ctrl)
      case FCGIStdErr(_, content) =>
        stderr(content)
        ctrl.resume()
      case _: FCGIEndRequest =>
        sendEOF()
        ctrl.resume()
    }
  }

  override def sendEOF() = if (!done) {
    done = true
    target.sendEOF()
  }
}
