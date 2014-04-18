package de.leanovate.moxie.framing

import de.leanovate.akka.tcp.PMPipe
import akka.util.ByteString
import play.api.libs.json.{Json, JsValue}

object Framing {
  def zeroTerminatedString = PMPipe.flatMapChunk(new ZeroTerminatedStringExtractor)

  def stringToJsValue = PMPipe.map[ByteString, JsValue] {
    json =>
      Json.parse(json.utf8String)
  }
}
