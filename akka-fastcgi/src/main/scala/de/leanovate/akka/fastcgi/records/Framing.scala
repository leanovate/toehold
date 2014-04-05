package de.leanovate.akka.fastcgi.records

import scala.concurrent.ExecutionContext
import play.api.libs.iteratee._
import akka.util.ByteString

object Framing {
  def toFCGIStdin(id: Int)(implicit ctx: ExecutionContext): Enumeratee[ByteString, FCGIRecord] =
    Enumeratee.mapInput[ByteString] {
      case Input.El(content) =>
        println("I1")
        Input.El(FCGIStdin(id, content))
      case Input.Empty =>
        Input.Empty
      case Input.EOF =>
        println("I2")
        Input.El(FCGIStdin(id, ByteString()))
    }

  def filterStdOut(stderr: ByteString => Unit)(implicit ctx: ExecutionContext) = Enumeratee.mapConcatInput[FCGIRecord] {
    case FCGIStdOut(_, content) =>
      Seq(Input.El(content))
    case FCGIStdErr(_, content) =>
      stderr(content)
      Seq.empty
    case _: FCGIEndRequest =>
      Seq(Input.EOF)
  }

  def bytesToRecords(implicit ctx: ExecutionContext) = Enumeratee.mapInputFlatten(new BytesToFCGIRecords)

  def headerLinesFromStdOut(implicit ctx: ExecutionContext) = {

    var buffer = ByteString.empty
    val lines = Seq.newBuilder[ByteString]
    def step(i: Input[ByteString]): Iteratee[ByteString, Seq[ByteString]] = i match {

      case Input.EOF =>
        if (buffer.isEmpty) {
          Done(lines.result(), Input.EOF)
        } else {
          Done(lines.result(), Input.El(buffer))
        }
      case Input.Empty => Cont(step)
      case Input.El(e) =>
        buffer ++= e
        var idx = buffer.indexOf('\n')
        var end = false

        while (idx >= 0) {
          val line = if (idx > 0 && buffer(idx - 1) == '\r') {
            buffer.take(idx - 1)
          } else {
            buffer.take(idx)
          }
          if (line.isEmpty) {
            end = true
          } else {
            lines += line
          }
          buffer = buffer.drop(idx + 1)
          idx = buffer.indexOf('\n')
        }
        if (end) {
          Done(lines.result(), Input.El(buffer))
        } else {
          Cont(step)
        }
    }
    Cont(step)
  }
}
