package de.leanovate.akka.fastcgi.request

import play.api.libs.iteratee.Enumerator
import akka.util.ByteString
import java.net.URI
import de.leanovate.akka.fastcgi.records._
import de.leanovate.akka.fastcgi.records.FCGIParams
import de.leanovate.akka.fastcgi.records.FCGIBeginRequest
import scala.concurrent.ExecutionContext

case class FCGIResponderRequest(method: String, path: String, query: String,
  documentRoot: String, headers: Map[String, Seq[String]], content: Enumerator[ByteString]) {
  def records(id: Int)(implicit ctx: ExecutionContext): Enumerator[FCGIRecord] = {

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
        headers.map {
          case (name, value) =>
            "HTTP" + name.toUpperCase.replace('-', '_') -> value.mkString(",")
        }.toSeq
      ).map(e => ByteString(e._1) -> ByteString(e._2)))

    Enumerator[FCGIRecord](beginRquest, params, FCGIParams(id, Seq.empty))
      .andThen(content &> Framing.toFCGIStdin(id))
  }
}
