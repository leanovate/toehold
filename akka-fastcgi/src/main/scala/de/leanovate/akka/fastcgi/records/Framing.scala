package de.leanovate.akka.fastcgi.records

import scala.concurrent.ExecutionContext
import play.api.libs.iteratee.{Input, Enumeratee}
import akka.util.ByteString

object Framing {
  def toFCGIStdin(id: Int)(implicit ctx: ExecutionContext): Enumeratee[ByteString, FCGIRecord] =
    Enumeratee.mapInput[ByteString] {
      case Input.El(content) =>
        Input.El(FCGIStdin(id, content))
      case Input.Empty =>
        Input.Empty
      case Input.EOF =>
        Input.El(FCGIStdin(id, ByteString()))
    }

  def fromFCGIStdout(implicit ctx: ExecutionContext) = Enumeratee.mapConcatInput[FCGIRecord] {
    case FCGIStdOut(_, content) => Seq(Input.El(content))
    case _: FCGIEndRequest => Seq(Input.EOF)
  }
}
