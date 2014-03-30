package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

import de.leanovate.akka.fastcgi.iteratee.RawWriter

trait FCGIRecord {
  def id: Short

  def typeId: Byte

  def content: ByteString

  def payload = ByteString(
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

  override def write(a: FCGIRecord) = a.payload
}