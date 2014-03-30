package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

case class FCGIStdin(id: Short, content: ByteString) extends FCGIRecord {
  override def typeId = 5
}
