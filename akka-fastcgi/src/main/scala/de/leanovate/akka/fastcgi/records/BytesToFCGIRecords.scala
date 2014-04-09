/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.records

import akka.util.ByteString
import de.leanovate.akka.iteratee.tcp.DataSink

class BytesToFCGIRecords(target: DataSink[FCGIRecord]) extends DataSink[ByteString] {
  private var buffer = ByteString.empty

  override def sendChunk(data: ByteString) = {
    buffer ++= data
    var extracted = FCGIRecord.decode(buffer)
    buffer = extracted._2
    while (extracted._1.isDefined) {
      target.sendChunk(extracted._1.get)
      extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
    }
  }

  override def sendEOF() = {
    var extracted = FCGIRecord.decode(buffer)
    buffer = extracted._2
    while (extracted._1.isDefined) {
      target.sendChunk(extracted._1.get)
      extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
    }
    target.sendEOF()
  }
}
