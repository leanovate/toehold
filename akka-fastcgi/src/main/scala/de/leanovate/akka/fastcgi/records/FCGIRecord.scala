package de.leanovate.akka.fastcgi.records

import akka.util.ByteString
import de.leanovate.akka.iteratee.tcp.RawWriter

trait FCGIRecord {
  def id: Int

  def typeId: Byte

  def content: ByteString

  def encode = ByteString(
                           FCGIRecord.FCGI_VERSION,
                           typeId,
                           (id >> 8).toByte, (id & 0xff).toByte,
                           ((content.length >> 8) & 0xff).toByte, (content.length & 0xff).toByte,
                           0.toByte,
                           0.toByte
                         ) ++ content
}

object FCGIRecord extends RawWriter[FCGIRecord] {
  val FCGI_VERSION = 1.toByte

  override def write(a: FCGIRecord) = a.encode

  def decode(data: ByteString): (Option[FCGIRecord], ByteString) = {

    if (data.length < 8) {
      (None, data)
    } else if (data(0) == 0) {
      (None, data.drop(1))
    } else {
      val len = ((data(4).toInt & 0xff) << 8) | (data(5).toInt & 0xff)
      if (data.length < len + 8) {
        (None, data)
      } else {
        val id = ((data(2).toInt & 0xff) << 8) | (data(3).toInt & 0xff)

        data(1) match {
          case FCGIConstants.FCGI_END_REQUEST =>
            (Some(FCGIEndRequest(id)), data.drop(len + 8))
          case FCGIConstants.FCGI_STDIN =>
            (Some(FCGIStdin(id, data.drop(8).take(len))), data.drop(len + 8).dropWhile(_ == 0))
          case FCGIConstants.FCGI_STDOUT =>
            (Some(FCGIStdOut(id, data.drop(8).take(len))), data.drop(len + 8).dropWhile(_ == 0))
          case FCGIConstants.FCGI_STDERR =>
            (Some(FCGIStdErr(id, data.drop(8).take(len))), data.drop(len + 8).dropWhile(_ == 0))
        }
      }
    }
  }
}