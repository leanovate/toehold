/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.request

import play.api.libs.iteratee.{Enumeratee, Iteratee, Enumerator}
import akka.util.ByteString
import de.leanovate.akka.fastcgi.records._
import de.leanovate.akka.fastcgi.records.FCGIParams
import de.leanovate.akka.fastcgi.records.FCGIBeginRequest
import scala.concurrent.ExecutionContext

case class FCGIResponderRequest(
  method: String,
  path: String,
  query: String,
  documentRoot: String,
  headers: Map[String, Seq[String]],
  optContent: Option[FCGIRequestContent]
  ) {

  def writeTo(id: Int, out: Iteratee[FCGIRecord, Any])(implicit ctx: ExecutionContext) {

    val beginRquest = FCGIBeginRequest(id, FCGIRoles.FCGI_AUTHORIZER, keepAlive = false)
    val params = FCGIParams(id, (
      Seq(
           "SCRIPT_FILENAME" -> (documentRoot + path),
           "QUERY_STRING" -> query,
           "REQUEST_METHOD" -> method,
           "SCRIPT_NAME" -> path,
           "REQUEST_URI" -> path,
           "DOCUMENT_URI" -> path,
           "DOCUMENT_ROOT" -> documentRoot,
           "SERVER_PROTOCOL" -> "HTTP/1.1",
           "GATEWAY_INTERFACE" -> "CGI/1.1"
         ) ++
        optContent.map {
          content =>
            Seq(
                 "CONTENT_TYPE" -> content.mimeType,
                 "CONTENT_LENGTH" -> content.length.toString
               )
        }.getOrElse(Seq.empty) ++
        headers.map {
          case (name, value) =>
            "HTTP_" + name.toUpperCase.replace('-', '_') -> value.mkString(",")
        }.toSeq
      ).map(e => ByteString(e._1) -> ByteString(e._2)))

    (Enumerator[FCGIRecord](beginRquest, params, FCGIParams(id, Seq.empty)) |>> out).foreach {
      nextOut =>
        optContent.map {
          content =>
            content.dataProvider(Enumeratee.map[Array[Byte]](ByteString(_))
              .transform(Framing.toFCGIStdin(id).transform(nextOut)))
        }.getOrElse {
          Enumerator(FCGIStdin(id, ByteString.empty): FCGIRecord) |>> nextOut
        }
    }
  }
}
