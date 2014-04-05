package de.leanovate.akka.fastcgi

import play.api.libs.iteratee.{Enumerator, Iteratee}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import akka.util.ByteString

trait FCGIConnectionHandler {
  def connected(out: Iteratee[FCGIRecord, Unit])

  def headerReceived(headers: Seq[(String, String)], in: Enumerator[ByteString])

  def connectionFailed()

}
