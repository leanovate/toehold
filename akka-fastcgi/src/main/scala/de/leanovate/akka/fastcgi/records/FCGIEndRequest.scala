package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

case class FCGIEndRequest(id : Int) extends FCGIRecord {
  override def typeId = FCGIConstants.FCGI_END_REQUEST

  override def content = ByteString()
}

