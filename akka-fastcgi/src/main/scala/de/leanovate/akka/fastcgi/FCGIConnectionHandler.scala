package de.leanovate.akka.fastcgi

import play.api.libs.iteratee.{Enumerator, Iteratee}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import akka.util.ByteString

trait FCGIConnectionHandler {
  def connected(in : Enumerator[FCGIRecord], out: Iteratee[FCGIRecord, Unit])

  def connectionFailed()

}
