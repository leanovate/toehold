/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.iteratee.framing

import scala.concurrent.ExecutionContext
import play.api.libs.iteratee.Enumeratee
import akka.util.ByteString

object Framing {
  def splitLines(implicit ec: ExecutionContext) = Enumeratee.mapInputFlatten(new SplitLines)

  def decode(charset: String)(implicit ec: ExecutionContext) = Enumeratee.map[ByteString](_.decodeString(charset))

  def decodeUTF8(implicit ec: ExecutionContext) = decode("UTF-8")
}
