/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.framing

import akka.util.ByteString
import de.leanovate.akka.tcp.PMSubscriber.{EOF, Data, Chunk}
import de.leanovate.akka.fastcgi.records.{FCGIEndRequest, FCGIStdErr, FCGIStdOut, FCGIRecord}

class FilterStdOut(stderr: ByteString => Unit) extends (Chunk[FCGIRecord] => Seq[Chunk[ByteString]]) {
  var done = false

  override def apply(chunk: Chunk[FCGIRecord]) =
    chunk match {
      case Data(data) =>
        data match {
          case FCGIStdOut(_, content) =>
            if (!done) {
              Seq(Data(content))
            } else {
              Seq.empty
            }
          case FCGIStdErr(_, content) =>
            stderr(content)
            Seq.empty
          case _: FCGIEndRequest =>
            if (!done) {
              done = true
              Seq(EOF)
            } else {
              Seq.empty
            }
        }
      case EOF =>
        if (!done) {
          done = true
          Seq(EOF)
        } else {
          Seq.empty
        }
    }
}
