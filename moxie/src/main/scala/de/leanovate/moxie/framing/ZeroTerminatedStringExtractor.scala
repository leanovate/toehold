package de.leanovate.moxie.framing

import de.leanovate.akka.tcp.PMStream.{Data, Chunk}
import akka.util.ByteString

class ZeroTerminatedStringExtractor extends (Chunk[ByteString] => Seq[Chunk[ByteString]]) {
  private var buffer = ByteString.empty

  override def apply(chunk: Chunk[ByteString]) = {

    chunk match {
      case Data(data) =>
        buffer ++= data
        Seq.empty
    }
  }
}
