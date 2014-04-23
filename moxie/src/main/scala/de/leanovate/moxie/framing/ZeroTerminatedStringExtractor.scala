/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.framing

import de.leanovate.akka.tcp.PMSubscriber.{EOF, Data, Chunk}
import akka.util.ByteString

class ZeroTerminatedStringExtractor extends (Chunk[ByteString] => Seq[Chunk[ByteString]]) {
  private var buffer = ByteString.empty

  override def apply(chunk: Chunk[ByteString]) = {

    chunk match {
      case Data(data) =>
        buffer ++= data
        val result = Seq.newBuilder[Chunk[ByteString]]
        Stream.continually(buffer.indexOf(0)).takeWhile(_ >= 0).foreach {
          idx =>
            result += Data(buffer.take(idx))
            buffer = buffer.drop(idx + 1)
        }
        result.result()
      case EOF =>
        if (buffer.isEmpty) {
          Seq(EOF)
        } else {
          Seq(Data(buffer), EOF)
        }
    }
  }
}
