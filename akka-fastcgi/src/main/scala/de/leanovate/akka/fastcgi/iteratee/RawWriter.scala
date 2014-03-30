package de.leanovate.akka.fastcgi.iteratee

import akka.util.ByteString

trait RawWriter[A] {
  def write(a: A): ByteString
}
