package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

case class FCGIStdOut(id: Int, content: ByteString) extends FCGIRecord {
  override def typeId = FCGIConstants.FCGI_STDOUT
}
