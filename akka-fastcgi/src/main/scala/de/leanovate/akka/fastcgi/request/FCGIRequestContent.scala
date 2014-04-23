/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.request

import akka.util.ByteString
import scala.concurrent.ExecutionContext
import de.leanovate.akka.tcp.AttachablePMConsumer

case class FCGIRequestContent(
  mimeType: String,
  length: Long,
  stream: AttachablePMConsumer[ByteString]
  )

object FCGIRequestContent {
  def apply(mimeType: String, str: String)(implicit ctx: ExecutionContext): FCGIRequestContent = {

    val data = ByteString(str)

    val content = new AttachablePMConsumer[ByteString]
    content.push(data)
    FCGIRequestContent(mimeType, data.length, content)
  }
}