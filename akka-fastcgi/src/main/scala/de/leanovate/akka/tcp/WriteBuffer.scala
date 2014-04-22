package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMStream.{Data, Control, Chunk}
import akka.util.ByteString
import scala.concurrent.stm.Ref
import akka.event.LoggingAdapter
import java.net.InetSocketAddress

class WriteBuffer(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress, log: LoggingAdapter) {
  private val buffer = Ref[Option[(Seq[Chunk[ByteString]], Control)]](None)

  def appendChunk(chunk: Chunk[ByteString], ctrl: Control): Option[(Seq[Chunk[ByteString]], Control)] =
    buffer.single.getAndTransform(atomicAppendChunk(chunk, ctrl))

  def takeChunk(): Option[(Seq[Chunk[ByteString]], Control)] =
    buffer.single.getAndTransform(atomicTakeChunk)

  private def atomicAppendChunk(chunk: Chunk[ByteString], ctrl: Control)
    (state: Option[(Seq[Chunk[ByteString]], Control)]): Option[(Seq[Chunk[ByteString]], Control)] =
    state match {
      case Some((chunks, _)) =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress push chunk to buffer")
        }
        (chunks, chunk) match {
          case (Seq(Data(remain)), Data(next)) =>
            Some(Seq(Data(remain ++ next)), ctrl)
          case _ =>
            Some(chunks :+ chunk, ctrl)
        }
      case None =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress push control to buffer")
        }
        Some(Seq.empty, ctrl)
    }

  private def atomicTakeChunk(
    state: Option[(Seq[Chunk[ByteString]], Control)]): Option[(Seq[Chunk[ByteString]], Control)] =
    state match {
      case Some((chunks, _)) if chunks.isEmpty =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress take last control from buffer")
        }
        None
      case Some((chunks, ctrl)) =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress take chunk from buffer")
        }
        Some(chunks.drop(1), ctrl)
      case None =>
        if (log.isDebugEnabled) {
          log.debug(s"$localAddress -> $remoteAddress buffer empty")
        }
        None
    }

}
