/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.records

import akka.util.ByteString
import de.leanovate.akka.iteratee.tcp.PMStream
import de.leanovate.akka.iteratee.tcp.PMStream.{EOF, Data, Control, Chunk}

class BytesToFCGIRecords extends (Chunk[ByteString] => Seq[Chunk[FCGIRecord]]) {
  private var buffer = ByteString.empty

  override def apply(chunk: Chunk[ByteString]) = {

    chunk match {
      case Data(data) =>
        buffer ++= data
        val records = Seq.newBuilder[Chunk[FCGIRecord]]
        var extracted = FCGIRecord.decode(buffer)
        buffer = extracted._2
        while (extracted._1.isDefined) {
          records += Data(extracted._1.get)
          extracted = FCGIRecord.decode(buffer)
          buffer = extracted._2
        }
        records.result()
      case EOF =>
        val records = Seq.newBuilder[Chunk[FCGIRecord]]
        var extracted = FCGIRecord.decode(buffer)
        buffer = extracted._2
        while (extracted._1.isDefined) {
          records += Data(extracted._1.get)
          extracted = FCGIRecord.decode(buffer)
          buffer = extracted._2
        }
        records += EOF
        records.result()
    }
  }
}
