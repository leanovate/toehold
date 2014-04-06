package de.leanovate.akka.fastcgi.request

import play.api.libs.iteratee.{Iteratee, Enumerator}
import akka.util.ByteString
import scala.concurrent.{ExecutionContext, Promise}

case class FCGIRequestContent(
  mimeType: String,
  length: Long,
  dataProvider: Iteratee[Array[Byte], _] => Unit
  )

object FCGIRequestContent {
  def apply(mimeType: String, str: String)(implicit ctx: ExecutionContext): FCGIRequestContent = {

    val data = ByteString(str)

    FCGIRequestContent(mimeType, data.length, {
      it =>
        Enumerator(data.toArray) |>>> it
    })
  }
}