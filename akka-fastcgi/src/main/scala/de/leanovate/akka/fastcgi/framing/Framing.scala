/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.framing

import akka.util.ByteString
import de.leanovate.akka.tcp.PMProcessor
import de.leanovate.akka.tcp.PMConsumer.{EOF, Data}
import de.leanovate.akka.fastcgi.records.{FCGIStdin, FCGIRecord}

object Framing {
  def byteArrayToByteString = PMProcessor.map[Array[Byte], ByteString](ByteString.apply)

  def toFCGIStdin(id: Int) = PMProcessor.mapChunk[ByteString, FCGIRecord] {
    case Data(content) =>
      Data(FCGIStdin(id, content))
    case EOF =>
      Data(FCGIStdin(id, ByteString.empty))
  }

  def bytesToFCGIRecords = PMProcessor.flatMapChunk(new BytesToFCGIRecords)

  def filterStdOut(stderr: ByteString => Unit) = PMProcessor.flatMapChunk(new FilterStdOut(stderr))
}
