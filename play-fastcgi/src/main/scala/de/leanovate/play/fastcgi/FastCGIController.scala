package de.leanovate.play.fastcgi

import play.api.mvc.{EssentialAction, Controller}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.Play.configuration
import de.leanovate.akka.fastcgi.FCGIRequestActor
import akka.pattern.ask
import de.leanovate.akka.fastcgi.request.FCGIRequestContent
import akka.util.{ByteString, Timeout}
import de.leanovate.akka.iteratee.adapt.PromiseEnumerator
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import play.api.libs.iteratee.Iteratee
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import scala.Some

object FastCGIController extends Controller {
  val fastCGIHost = configuration.getString("fastcgi.host").getOrElse("localhost")

  val fastCGIPort = configuration.getInt("fastcgi.port").getOrElse(9001)

  implicit val fastGGITimeout = Timeout(configuration.getMilliseconds("fastcgi.timeout").map(_.milliseconds)
    .getOrElse(60.seconds))

  val fcgiRequestActor = Akka.system.actorOf(FCGIRequestActor.props(fastCGIHost, fastCGIPort))

  def serve(documentRoot: String, path: String) = EssentialAction {
    requestHeader =>
      requestHeader.contentType.map {
        contentType =>
          requestHeader.headers.get("content-length").map {
            contentLength =>
              val content = new PromiseEnumerator[Array[Byte]]


              content.promisedIteratee.map(_ => Ok("bla"))
          }.getOrElse {
            Iteratee.ignore[Array[Byte]].map {
              _ => new Status(LENGTH_REQUIRED)
            }
          }
      }.getOrElse {
        val content = new PromiseEnumerator[Array[Byte]]

        fcgiRequestActor ? FCGIResponderRequest(
                                                 requestHeader.method,
                                                 path,
                                                 requestHeader.rawQueryString,
                                                 documentRoot,
                                                 requestHeader.headers.toMap,
                                                 None
                                               )

        content.promisedIteratee.map(_ => Ok("bla"))

      }

  }
}
