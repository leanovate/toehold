package de.leanovate.spray.fastcgi

import akka.actor.{ActorRef, Actor}
import spray.http._
import de.leanovate.akka.fastcgi.request._
import spray.http.HttpRequest
import spray.http.HttpResponse
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import de.leanovate.akka.fastcgi.request.FCGIResponderError
import scala.collection.mutable
import de.leanovate.spray.tcp.HttpOutStreamActor

trait FastCGISupport {
  actor: Actor =>
  private val extension = FastCGIExtension(context.system)

  def fastcgiReceive: Actor.Receive = {
    case HttpRequest(method, uri, headers, HttpEntity.Empty, _) =>
      val scriptPath = scriptPathFromUri(uri)
      val request = FCGIResponderRequest(
        method.value,
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

  def scriptPathFromUri(uri: Uri): String = {
    uri.path.toString()
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

  def additionalEnv: Seq[(String, String)] = Seq.empty
}
