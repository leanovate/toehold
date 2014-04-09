package de.leanovate.akka.iteratee.tcp

trait DataSink[A] {
  def sendChunk(data: A)

  def sendEOF()
}
