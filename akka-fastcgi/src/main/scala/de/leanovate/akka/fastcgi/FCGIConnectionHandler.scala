/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import play.api.libs.iteratee.{Enumerator, Iteratee}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import akka.util.ByteString

trait FCGIConnectionHandler {
  def connected(out: Iteratee[FCGIRecord, Unit])

  def headerReceived(statusCode: Int, statusLine: String, headers: Seq[(String, String)], in: Enumerator[ByteString])

  def connectionFailed()

}
