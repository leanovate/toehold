package de.leanovate.akka.fastcgi.records

import scala.concurrent.ExecutionContext
import play.api.libs.iteratee.{Input, Enumeratee}
import akka.util.ByteString

object Framing {
  def toFCGIStdin(id: Int)(implicit ctx: ExecutionContext) = Enumeratee.map[ByteString] {
    content =>
      FCGIStdin(id, content)
  }

  def fromFCGIStdout(implicit ctx: ExecutionContext) = Enumeratee.mapConcatInput[FCGIRecord] {
    case FCGIStdOut(_, content) => Seq(Input.El(content))
    case _: FCGIEndRequest => Seq(Input.EOF)
  }
}
