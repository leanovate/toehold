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

class RawRecord(encoded: Array[Byte]) extends FCGIRecord {
  override def content = ???

  override def typeId = ???

  override def id = ???

  override def encode = ByteString(encoded)
}

object FCGIRecord extends RawWriter[FCGIRecord] {
  val FCGI_VERSION = 1.toByte

  override def write(a: FCGIRecord) = a.encode

  def decode(data: ByteString): (Option[FCGIRecord], ByteString) = {

    if (data.length < 8) {
      (None, data)
    } else {
      val len = ((data(4).toInt & 0xff) << 8) | (data(5).toInt & 0xff)
      val pad = data(6).toInt & 0xff
      if (data.length < len + pad + 8) {
        (None, data)
      } else {
        val id = ((data(2).toInt & 0xff) << 8) | (data(3).toInt & 0xff)
        val content = data.drop(8).take(len)
        val remain = data.drop(len + pad + 8)

        data(1) match {
          case FCGIConstants.FCGI_BEGIN_REQUEST =>
            (Some(FCGIBeginRequest.read(id, content)), remain)
          case FCGIConstants.FCGI_END_REQUEST =>
            (Some(FCGIEndRequest(id)), remain)
          case FCGIConstants.FCGI_STDIN =>
            (Some(FCGIStdin(id, content)), remain)
          case FCGIConstants.FCGI_STDOUT =>
            (Some(FCGIStdOut(id, content)), remain)
          case FCGIConstants.FCGI_STDERR =>
            (Some(FCGIStdErr(id, content)), remain)
        }
      }
    }
  }
}