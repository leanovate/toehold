/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import play.api.mvc.{ResponseHeader, SimpleResult, EssentialAction, Controller}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.Play.configuration
import de.leanovate.akka.fastcgi.request.{FCGIResponderError, FCGIResponderSuccess, FCGIRequestContent, FCGIResponderRequest}
import akka.util.{ByteString, Timeout}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import play.api.libs.iteratee.Iteratee
import scala.Some
import scala.concurrent.Promise
import akka.pattern.ask
import de.leanovate.play.tcp.{IterateeAdapter, EnumeratorAdapter}
import de.leanovate.akka.tcp.AttachablePMStream
import de.leanovate.akka.fastcgi.framing.Framing

object FastCGIController extends Controller {
  implicit val fastGGITimeout = Timeout(configuration.getMilliseconds("fastcgi.timeout").map(_.milliseconds)
    .getOrElse(60.seconds))

  val fastCGIActor = Akka.system.actorSelection("/user/" + FastCGISupport.FASTCGI_ACTOR_NAME)

  def serve(documentRoot: String, path: String, extension: String) = EssentialAction {
    requestHeader =>
      requestHeader.contentType.map {
        contentType =>
          requestHeader.headers.get("content-length").map {
            contentLength =>
              val requestContentStream = new AttachablePMStream[ByteString]
              val requestContent = FCGIRequestContent(contentType, contentLength.toLong, requestContentStream)
              val request = FCGIResponderRequest(
                                                  requestHeader.method,
                                                  "/" + path + extension,
                                                  requestHeader.rawQueryString,
                                                  documentRoot,
                                                  requestHeader.headers.toMap,
                                                  Some(requestContent)
                                                )
              val resultPromise = Promise[SimpleResult]()
              (fastCGIActor ? request).map {
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
                                            "/" + path + extension,
                                            requestHeader.rawQueryString,
                                            documentRoot,
                                            requestHeader.headers.toMap,
                                            None
                                          )

        Iteratee.ignore[Array[Byte]].mapM {
          _ =>
            (fastCGIActor ? request).map {
              case FCGIResponderSuccess(statusCode, statusLine, headers, content) =>
                val contentEnum = EnumeratorAdapter.adapt(content).map(_.toArray)
                SimpleResult(ResponseHeader(statusCode, headers.toMap), contentEnum)
              case FCGIResponderError(msg) =>
                InternalServerError(msg)
            }
        }
      }
  }
}