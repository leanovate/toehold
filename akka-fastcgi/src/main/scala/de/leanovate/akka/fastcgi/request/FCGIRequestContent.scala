/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.request

import play.api.libs.iteratee.{Iteratee, Enumerator}
import akka.util.ByteString
import scala.concurrent.{ExecutionContext, Promise}
import de.leanovate.akka.tcp.AttachablePMStream

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