/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.spray.fastcgi

import akka.actor.{ActorRef, Actor}
import spray.http._
import de.leanovate.akka.fastcgi.request._
import scala.collection.mutable
import de.leanovate.spray.tcp.HttpOutStreamActor
import java.io.File
import spray.http.HttpMethods.GET
import spray.http.HttpEntity.Empty
import de.leanovate.akka.tcp.StreamPMPublisher
import akka.util.ByteString
import spray.http.HttpRequest
import de.leanovate.akka.fastcgi.request.FCGIResponderSuccess
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import de.leanovate.akka.fastcgi.request.FCGIResponderError
import scala.Some
import spray.http.HttpEntity.NonEmpty
import spray.http.HttpResponse
import spray.http.HttpHeaders.{`If-None-Match`, ETag, `Last-Modified`, `If-Modified-Since`}

trait FastCGISupport {
  actor: Actor =>
  private val extension = FastCGIExtension(context.system)

  val phpExtensions = Seq("php", "php5", "php4", "php3")

  def fastcgiReceive: Actor.Receive = {
    case HttpRequest(GET, uri, headers, _, _) =>
      scriptOrFileFromUri(uri) match {
        case Some(Left(scriptPath)) =>
          val request = FCGIResponderRequest(
            GET.value,
            scriptPath,
            uri.path.toString(),
            uri.query.toString(),
            extension.settings.documentRoot,
            mapHeaders(headers),
            additionalEnv,
            None,
            ref = sender
          )

          extension.requestActor ! request
        case Some(Right(file)) if file.exists() && file.canRead =>
          val notModified = headers.exists {
            case `If-None-Match`(tags) =>
              val etag = calcEtag(file)
              EntityTag.matchesRange(EntityTag(etag), tags, weak = false)
            case `If-Modified-Since`(date) =>
              date >= DateTime(file.lastModified())
            case _ => false
          }
          if (notModified) {
            sender ! HttpResponse(status = StatusCodes.NotModified)
          } else {
            sender ! HttpResponse(
              headers = ETag(calcEtag(file)) :: `Last-Modified`(DateTime(file.lastModified())) :: Nil,
              entity = HttpData(file)
            )
          }
        case _ =>
          sender ! HttpResponse(status = StatusCodes.NotFound)
      }

    case HttpRequest(method, uri, headers, entity, _) =>
      scriptFromUri(uri) match {
        case Some(scriptPath) =>
          val request = FCGIResponderRequest(
            method.value,
            scriptPath,
            uri.path.toString(),
            uri.query.toString(),
            extension.settings.documentRoot,
            mapHeaders(headers),
            additionalEnv,
            mapEntity(entity),
            ref = sender
          )

          extension.requestActor ! request
        case _ =>
          sender ! HttpResponse(status = StatusCodes.NotFound)
      }

    case FCGIResponderSuccess(statusCode, statusLine, headers, content, connection: ActorRef) =>
      connection ! HttpResponse(status = StatusCodes.getForKey(statusCode).getOrElse {
        StatusCodes.registerCustom(statusCode, statusLine, statusLine)
      }, headers = mapHeaders(headers)).chunkedMessageStart

      context.actorOf(HttpOutStreamActor.props(content, connection))

    case FCGIResponderError(msg, connection: ActorRef) =>
      connection ! HttpResponse(status = StatusCodes.InternalServerError, entity = msg)
  }

  def scriptFromUri(uri: Uri): Option[String] = {
    val path = uri.path.toString()

    getFileExtension(path).flatMap {
      case fileExtension if phpExtensions.contains(fileExtension) =>
        Some(path)
      case _ =>
        None
    }
  }

  def scriptOrFileFromUri(uri: Uri): Option[Either[String, File]] = {
    val path = uri.path.toString()

    getFileExtension(path).flatMap {
      case fileExtension if phpExtensions.contains(fileExtension) =>
        Some(Left(path))
      case fileExtension if extension.settings.fileWhiteList.contains(fileExtension) =>
        Some(Right(new File(extension.settings.documentRoot, path)))
      case _ =>
        None
    }
  }

  def mapHeaders(headers: Seq[(String, String)]): List[HttpHeader] = {
    headers.map {
      case (name, value) =>
        FastCGIHeader(name, value)
    }.toList
  }

  def mapHeaders(headers: List[HttpHeader]): Map[String, Seq[String]] = {
    val result = mutable.Map.empty[String, Seq[String]]

    headers.foreach {
      header =>
        val values = result.getOrElse(header.lowercaseName, Seq.empty)

        result.update(header.lowercaseName, values :+ header.value)
    }
    result.toMap
  }

  def mapEntity(entity: HttpEntity): Option[FCGIRequestContent] = {
    entity match {
      case Empty =>
        None
      case NonEmpty(contentType, data) =>
        Some(FCGIRequestContent(
          contentType.toString(),
          data.length,
          new StreamPMPublisher[ByteString](data.toChunkStream(8192L).map(_.toByteString))))
    }
  }

  def additionalEnv: Seq[(String, String)] = Seq.empty

  def getFileExtension(str: String): Option[String] = {
    val idx = str.lastIndexOf('.')
    if (idx > 0)
      Some(str.substring(idx + 1).toLowerCase)
    else
      None
  }

  def calcEtag(file: File): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    digest.update(file.getName.getBytes("UTF-8"))
    digest.update(file.lastModified().toString.getBytes("UTF-8"))
    digest.update(file.length().toString.getBytes("UTF-8"))
    digest.digest().map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
  }

  case class FastCGIHeader(name: String, value: String) extends HttpHeader {
    val lowercaseName = name.toLowerCase

    def render[R <: Rendering](r: R): r.type = r ~~ name ~~ ':' ~~ ' ' ~~ value
  }

}
