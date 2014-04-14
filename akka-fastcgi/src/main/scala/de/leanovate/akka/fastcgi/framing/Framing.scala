/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.framing

import akka.util.ByteString
import de.leanovate.akka.tcp.PMPipe
import de.leanovate.akka.tcp.PMStream.{EOF, Data}
import de.leanovate.akka.fastcgi.records.{FCGIStdin, FCGIRecord}

object Framing {
  def byteArrayToByteString = PMPipe.map[Array[Byte], ByteString](ByteString.apply)

  def toFCGIStdin(id: Int) = PMPipe.mapChunk[ByteString, FCGIRecord] {
    case Data(content) =>
      Data(FCGIStdin(id, content))
    case EOF =>
      Data(FCGIStdin(id, ByteString.empty))
  }

}
