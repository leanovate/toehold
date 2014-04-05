package de.leanovate.akka.iteratee.tcp

trait FeedSink[A] {
  def feedChunk(data: A)

  def feedEOF()
}
