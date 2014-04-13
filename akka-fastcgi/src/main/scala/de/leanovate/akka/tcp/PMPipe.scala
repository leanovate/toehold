package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMStream._
import de.leanovate.akka.tcp.PMStream.Data

trait PMPipe[From, To] {
  def |>(target: PMStream[To]): PMStream[From]

  def |>[A](other: PMPipe[To, A]) = new PMPipe.ConcatPipe(this, other)
}

object PMPipe {
  def map[From, To](f: From => To) = new PMPipe[From, To] {
    override def |>(target: PMStream[To]) = new PMStream[From] {
      override def send(chunk: Chunk[From], ctrl: Control) = {

        chunk match {
          case Data(data) =>
            target.send(Data(f(data)), ctrl)
          case EOF =>
            target.send(EOF, ctrl)
        }
      }
    }
  }

  def flatMap[From, To](f: Chunk[From] => Seq[Chunk[To]]) = new PMPipe[From, To] {
    override def |>(target: PMStream[To]) = new PMStream[From] {
      override def send(chunk: Chunk[From], ctrl: Control) = {

        target.send(f(chunk), ctrl)
      }
    }
  }

  class ConcatPipe[From, Mid, To](in: PMPipe[From, Mid], out: PMPipe[Mid, To]) extends PMPipe[From, To] {
    override def |>(target: PMStream[To]) = in |> (out |> target)
  }

}