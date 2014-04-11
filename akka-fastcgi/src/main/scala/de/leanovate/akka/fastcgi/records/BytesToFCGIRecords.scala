/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.records

import akka.util.ByteString
import de.leanovate.akka.iteratee.tcp.PMStream

class BytesToFCGIRecords(target: PMStream[FCGIRecord]) extends PMStream[ByteString] {
  private var buffer = ByteString.empty

  override def sendChunk(data: ByteString, ctrl: PMStream.Control) = {

    buffer ++= data
    var extracted = FCGIRecord.decode(buffer)
    val countdown = new PMStream.CountdownResumer(ctrl)
    buffer = extracted._2
    while (extracted._1.isDefined) {
      countdown.increment()
      target.sendChunk(extracted._1.get, countdown)
      extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
    }
    countdown.resume()
  }

  override def sendEOF() = {

    var extracted = FCGIRecord.decode(buffer)
    buffer = extracted._2
    while (extracted._1.isDefined) {
      target.sendChunk(extracted._1.get, PMStream.EmptyControl)
      extracted = FCGIRecord.decode(buffer)
      buffer = extracted._2
    }
    target.sendEOF()
  }
}
