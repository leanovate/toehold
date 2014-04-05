package de.leanovate.akka.iteratee.tcp

import akka.util.ByteString

trait FeedSink[A] {
  def feedChunk(data: ByteString)

  def feedEOF()
}
