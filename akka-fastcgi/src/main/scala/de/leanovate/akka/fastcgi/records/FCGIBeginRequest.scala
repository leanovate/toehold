package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

case class FCGIBeginRequest(id: Int, role: FCGIRoles.Type, keepAlive: Boolean) extends FCGIRecord {
  override def typeId = FCGIConstants.FCGI_BEGIN_REQUEST

  override def content = ByteString(
                                     ((role.id >> 8) & 0xff).toByte, (role.id & 0xff).toByte,
                                     if (keepAlive) {
                                       1.toByte
                                     } else {
                                       0.toByte
                                     },
                                     0.toByte,
                                     0.toByte,
                                     0.toByte,
                                     0.toByte,
                                     0.toByte
                                   )
}

object FCGIBeginRequest {
  def read(id: Int, content: ByteString) =
    FCGIBeginRequest(id, FCGIRoles(((content(0).toInt & 0xff) << 8) | (content(0).toInt & 0xff)), content(3) != 0)
}