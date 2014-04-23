package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMSubscriber.{Data, Chunk}
import akka.util.ByteString
import scala.concurrent.stm.Ref
import akka.event.LoggingAdapter
import java.net.InetSocketAddress

class WriteBuffer(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress, log: LoggingAdapter) {
  private val buffer = Ref[Option[Seq[Chunk[ByteString]]]](None)

  def appendChunk(chunk: Chunk[ByteString]): Option[Seq[Chunk[ByteString]]] =
    buffer.single.getAndTransform(atomicAppendChunk(chunk))

  def takeChunk(): Option[Seq[Chunk[ByteString]]] =
    buffer.single.getAndTransform(atomicTakeChunk)

  private def atomicAppendChunk(chunk: Chunk[ByteString])
    (state: Option[Seq[Chunk[ByteString]]]): Option[Seq[Chunk[ByteString]]] =
    state match {
      case Some(chunks) =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress push chunk to buffer")
        }
        (chunks, chunk) match {
          case (Seq(Data(remain)), Data(next)) =>
            Some(Seq(Data(remain ++ next)))
          case _ =>
            Some(chunks :+ chunk)
        }
      case None =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress push control to buffer")
        }
        Some(Seq.empty)
    }

  private def atomicTakeChunk(
    state: Option[Seq[Chunk[ByteString]]]): Option[Seq[Chunk[ByteString]]] =
    state match {
      case Some(chunks) if chunks.isEmpty =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress take last control from buffer")
        }
        None
      case Some(chunks) =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress take chunk from buffer")
        }
        Some(chunks.drop(1))
      case None =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress buffer empty")
        }
        None
    }

}
