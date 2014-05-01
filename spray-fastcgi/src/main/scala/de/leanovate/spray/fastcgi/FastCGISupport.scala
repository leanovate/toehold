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

          println("S: " + sender)
          println("Send " + request)
          extension.requestActor ! request
        case Some(Right(file)) if file.exists() && file.canRead =>
          sender ! HttpResponse(entity = HttpData(file))
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

          println("S2: " + sender)
          println("Send2 " + request)
          extension.requestActor ! request
        case _ =>
          sender ! HttpResponse(status = StatusCodes.NotFound)
      }

    case FCGIResponderSuccess(statusCode, statusLine, headers, content, connection: ActorRef) =>
      println("Res: " + connection)
      connection ! HttpResponse(status = StatusCodes.getForKey(statusCode).getOrElse {
        StatusCodes.registerCustom(statusCode, statusLine, statusLine)
      }, headers = mapHeaders(headers)).chunkedMessageStart

      context.actorOf(HttpOutStreamActor.props(content, connection))

    case FCGIResponderError(msg, connection: ActorRef) =>
      println("Res2: " + connection)
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
      case (_name, _value) =>
        new HttpHeader() {
          override def name = _name

          override def value = _value

          override def lowercaseName = _name.toLowerCase

          override def render[R <: Rendering](r: R): r.type = r ~~ _name ~~ ':' ~~ ' '
        }
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
          new StreamPMPublisher[ByteString](Stream(data.toByteString))))
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
}
