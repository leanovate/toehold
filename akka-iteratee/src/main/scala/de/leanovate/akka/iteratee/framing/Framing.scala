package de.leanovate.akka.iteratee.framing

import scala.concurrent.ExecutionContext
import play.api.libs.iteratee.Enumeratee
import akka.util.ByteString

object Framing {
  def splitLines(implicit ec: ExecutionContext) = Enumeratee.mapInputFlatten(new SplitLines)

  def decode(charset: String)(implicit ec: ExecutionContext) = Enumeratee.map[ByteString](_.decodeString(charset))

  def decodeUTF8(implicit ec: ExecutionContext) = decode("UTF-8")
}
