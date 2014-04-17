/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import play.api.mvc.{ResponseHeader, SimpleResult, EssentialAction, Controller}
import play.api.Play.current
import de.leanovate.akka.fastcgi.request.{FCGIResponderError, FCGIResponderSuccess, FCGIRequestContent, FCGIResponderRequest}
import akka.util.ByteString
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Iteratee
import scala.Some
import scala.concurrent.Promise
import akka.pattern.ask
import de.leanovate.play.tcp.{IterateeAdapter, EnumeratorAdapter}
import de.leanovate.akka.tcp.AttachablePMStream
import de.leanovate.akka.fastcgi.framing.Framing
import java.io.File
import akka.actor.ActorRef

trait FastCGIController extends Controller {
  def serveFromUri(path: String, extension: String = "", documentRoot: Option[String] = None): EssentialAction =
    serveScript(path + extension, path + extension, documentRoot)

  def serveScript(scriptName: String, uri: String, documentRoot: Option[String] = None,
    additionalEnv: Seq[(String, String)] = Seq.empty) = EssentialAction {
    requestHeader =>
      implicit val timeout = settings.timeout
      requestHeader.contentType.map {
        contentType =>
          requestHeader.headers.get("content-length").map {
            contentLength =>
              val requestContentStream = new AttachablePMStream[ByteString]
              val requestContent = FCGIRequestContent(contentType, contentLength.toLong, requestContentStream)
              val request = FCGIResponderRequest(
                                                  requestHeader.method,
                                                  "/" + scriptName,
                                                  "/" + uri,
                                                  requestHeader.rawQueryString,
                                                  documentRoot.map(new File(_)).getOrElse(settings.documentRoot),
                                                  requestHeader.headers.toMap,
                                                  additionalEnv,
                                                  Some(requestContent)
                                                )
              val resultPromise = Promise[SimpleResult]()
              (requestActor ? request).map {
                case FCGIResponderSuccess(statusCode, statusLine, headers, content) =>
                  val contentEnum = EnumeratorAdapter.adapt(content).map(_.toArray)
                  resultPromise.success(SimpleResult(ResponseHeader(statusCode, headers.toMap), contentEnum))
                case FCGIResponderError(msg) =>
                  resultPromise.success(InternalServerError(msg))
              }

              IterateeAdapter.adapt(Framing.byteArrayToByteString |> requestContentStream).mapM {
                _ =>
                  resultPromise.future
              }
          }.getOrElse {
            Iteratee.ignore[Array[Byte]].map {
              _ => new Status(LENGTH_REQUIRED)
            }
          }
      }.getOrElse {
        val request = FCGIResponderRequest(
                                            requestHeader.method,
                                            "/" + scriptName,
                                            "/" + uri,
                                            requestHeader.rawQueryString,
                                            documentRoot.map(new File(_)).getOrElse(settings.documentRoot),
                                            requestHeader.headers.toMap,
                                            additionalEnv,
                                            None
                                          )

        Iteratee.ignore[Array[Byte]].mapM {
          _ =>
            (requestActor ? request).map {
              case FCGIResponderSuccess(statusCode, statusLine, headers, content) =>
                val contentEnum = EnumeratorAdapter.adapt(content).map(_.toArray)
                SimpleResult(ResponseHeader(statusCode, headers.toMap), contentEnum)
              case FCGIResponderError(msg) =>
                InternalServerError(msg)
            }
        }
      }
  }

  protected def settings: FastCGISettings = FastCGIPlugin.settings

  protected def requestActor: ActorRef = FastCGIPlugin.requestActor
}

object FastCGIController extends FastCGIController