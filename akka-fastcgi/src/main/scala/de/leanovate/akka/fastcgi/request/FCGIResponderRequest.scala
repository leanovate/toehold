/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.request

import akka.util.ByteString
import de.leanovate.akka.fastcgi.records._
import de.leanovate.akka.fastcgi.records.FCGIParams
import de.leanovate.akka.fastcgi.records.FCGIBeginRequest
import scala.concurrent.ExecutionContext
import de.leanovate.akka.tcp.PMSubscriber
import de.leanovate.akka.fastcgi.framing.Framing
import java.io.File

case class FCGIResponderRequest(
                                 method: String,
                                 scriptName: String,
                                 uri: String,
                                 query: String,
                                 documentRoot: File,
                                 headers: Map[String, Seq[String]],
                                 additionalEnv: Seq[(String, String)],
                                 optContent: Option[FCGIRequestContent]
                                 ) extends FCGIRequest {

  override def writeTo(id: Int, out: PMSubscriber[FCGIRecord]) {

    val beginRquest = FCGIBeginRequest(id, FCGIRoles.FCGI_AUTHORIZER, keepAlive = false)
    val params = FCGIParams(id, (
      Seq(
        "SCRIPT_FILENAME" -> (documentRoot.getCanonicalPath + scriptName),
        "QUERY_STRING" -> query,
        "REQUEST_METHOD" -> method,
        "SCRIPT_NAME" -> scriptName,
        "REQUEST_URI" -> uri,
        "DOCUMENT_URI" -> uri,
        "DOCUMENT_ROOT" -> documentRoot.getCanonicalPath,
        "SERVER_PROTOCOL" -> "HTTP/1.1",
        "GATEWAY_INTERFACE" -> "CGI/1.1"
      ) ++
        optContent.fold(Seq.empty[(String, String)]) {
          content =>
            Seq(
              "CONTENT_TYPE" -> content.mimeType,
              "CONTENT_LENGTH" -> content.length.toString
            )
        } ++
        headers.map {
          case (name, value) =>
            "HTTP_" + name.toUpperCase.replace('-', '_') -> value.mkString(",")
        }.toSeq ++
        additionalEnv
      ).map(e => ByteString(e._1) -> ByteString(e._2)))

    out.push(beginRquest, params, FCGIParams(id, Seq.empty))
    optContent.fold(out.push(FCGIStdin(id, ByteString.empty))) {
      content =>
        content.stream.subscribe(Framing.toFCGIStdin(id) |> out)
    }
  }
}
