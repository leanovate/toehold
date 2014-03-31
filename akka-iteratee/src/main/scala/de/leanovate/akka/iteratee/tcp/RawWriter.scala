package de.leanovate.akka.iteratee.tcp

import akka.util.ByteString

trait RawWriter[A] {
  def write(a: A): ByteString
}
