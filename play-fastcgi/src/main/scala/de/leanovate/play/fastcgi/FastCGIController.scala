/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import play.api.mvc._
import play.api.Play.current
import de.leanovate.akka.fastcgi.request._
import akka.util.ByteString
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Iteratee
import scala.concurrent.{Future, Promise}
import akka.pattern.ask
import de.leanovate.play.tcp.{IterateeAdapter, EnumeratorAdapter}
import de.leanovate.akka.fastcgi.framing.Framing
import java.io.File
import akka.actor.ActorRef
import de.leanovate.akka.fastcgi.request.FCGIResponderSuccess
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import de.leanovate.akka.fastcgi.request.FCGIResponderError
import scala.Some
import de.leanovate.akka.tcp.AttachablePMSubscriber
import play.api.mvc.SimpleResult
import play.api.mvc.ResponseHeader
import play.api.libs.json.{JsNumber, JsObject}

trait FastCGIController extends Controller {
  def serveFromUri(path: String, extension: String = "", documentRoot: Option[String] = None): EssentialAction =
    serveScript(path + extension, path + extension, documentRoot)

  def serveScript(scriptName: String, uri: String, documentRoot: Option[String] = None,
    additionalEnv: Seq[(String, String)] = Seq.empty) = EssentialAction {
    requestHeader =>
      implicit val timeout = settings.requestTimeout
      requestHeader.contentType.map {
        contentType =>
          requestHeader.headers.get("content-length").map {
            contentLength =>
              val requestContentStream = new AttachablePMSubscriber[ByteString]
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
              val resultFuture = requestActor ? request

              IterateeAdapter.adapt(Framing.byteArrayToByteString |> requestContentStream).mapM {
                _ => mapResultFuture(resultFuture)
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
        val resultFuture = requestActor ? request

        Iteratee.ignore[Array[Byte]].mapM {
          _ => mapResultFuture(resultFuture)
        }
      }
  }

  def status = Action.async {
    implicit val timeout = settings.requestTimeout
    (requestActor ? FCGIQueryStatus).map {
      case status: FCGIStatus =>
        Ok(JsObject(Seq("openConnections" -> JsNumber(status.openConnections))))
    }
  }

  protected def mapResultFuture(resultFuture: Future[Any]): Future[SimpleResult] = {

    resultFuture.map {
      case FCGIResponderSuccess(statusCode, statusLine, headers, content) =>
        val contentEnum = EnumeratorAdapter.adapt(content).map(_.toArray)
        SimpleResult(ResponseHeader(statusCode, headers.toMap), contentEnum)
      case FCGIResponderError(msg) =>
        InternalServerError(msg)
    }
  }

  protected def settings: FastCGISettings = FastCGIPlugin.settings

  protected def requestActor: ActorRef = FastCGIPlugin.requestActor
}

object FastCGIController extends FastCGIController