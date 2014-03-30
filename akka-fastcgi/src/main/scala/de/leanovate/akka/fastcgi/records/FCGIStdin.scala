package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

case class FCGIStdin(id: Int, content: ByteString) extends FCGIRecord {
  override def typeId = FCGIConstants.FCGI_STDIN
}
