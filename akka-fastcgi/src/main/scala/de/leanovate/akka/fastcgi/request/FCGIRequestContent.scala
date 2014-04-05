package de.leanovate.akka.fastcgi.request

import play.api.libs.iteratee.Enumerator
import akka.util.ByteString

case class FCGIRequestContent(
  mimeType: String,
  length: Long,
  data: Enumerator[ByteString]
  )

object FCGIRequestContent {
  def apply(mimeType: String, str: String): FCGIRequestContent = {

    val data = ByteString(str)
    FCGIRequestContent(mimeType, data.length, Enumerator(data))
  }
}