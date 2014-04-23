/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.framing

import de.leanovate.akka.tcp.PMProcessor
import akka.util.ByteString
import play.api.libs.json.{Json, JsValue}

object Framing {
  def zeroTerminatedString = PMProcessor.flatMapChunk(new ZeroTerminatedStringExtractor)

  def bytesToJsValue = PMProcessor.map[ByteString, JsValue] {
    json =>
      Json.parse(json.utf8String)
  }
}
