package de.leanovate.akka.iteratee.tcp

import akka.util.ByteString

trait RawWriter[A] {
  def write(a: A): ByteString
}

object RawWriter {
  val raw = new RawWriter[ByteString] {
    def write(a: ByteString) = a
  }
}