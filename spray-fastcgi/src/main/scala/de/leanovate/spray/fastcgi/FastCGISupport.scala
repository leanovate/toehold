package de.leanovate.spray.fastcgi

import akka.actor.{ActorContext, ActorRef, Actor}
import spray.http._
import de.leanovate.akka.fastcgi.request._
import spray.http.HttpRequest
import spray.http.HttpResponse
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import de.leanovate.akka.fastcgi.request.FCGIResponderError
import scala.collection.mutable
import de.leanovate.spray.tcp.HttpOutPMSubscriber
import akka.io.Tcp.ConnectionClosed

trait FastCGISupport {
  actor: Actor =>
  private val extension = context.system.extension(FastCGIExtension)
  private val openRequests = mutable.Map.empty[ActorRef, ConnectionState]

  def fastcgiReceive: Actor.Receive = {
    case HttpRequest(method, uri, headers, HttpEntity.Empty, _) =>
      openRequests.put(context.sender, ConnectionState(None))
      val scriptName = scriptNameFromUri(uri)
      val request = FCGIResponderRequest(
        method.value,
        "/" + scriptName,
        "/" + uri.path,
        uri.query.value,
        extension.settings.documentRoot,
        mapHeaders(headers),
        additionalEnv,
        None,
        ref = context.sender
      )

      extension.requestActor ! request

    case FCGIResponderSuccess(statusCode, statusLine, headers, content, connection: ActorRef) =>
      connection ! HttpResponse(status = StatusCodes.getForKey(statusCode).getOrElse {
        StatusCodes.registerCustom(statusCode, statusLine, statusLine)
      }, headers = mapHeaders(headers)).chunkedMessageStart

      val httpOut = new HttpOutPMSubscriber(sender)
      content.subscribe(httpOut)
      openRequests.put(sender, ConnectionState(Some(httpOut)))

    case FCGIResponderError(msg, connection: ActorRef) =>
      connection ! HttpResponse(status = StatusCodes.InternalServerError, entity = msg)

    case HttpOutPMSubscriber.WriteAck =>
      openRequests.get(sender).foreach(_.httpOut.foreach(_.ackknowledge()))

    case _: ConnectionClosed =>
      openRequests.remove(sender)
  }

  def scriptNameFromUri(uri: Uri): String = {
    extension.settings.documentRoot.getCanonicalPath + uri.path
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

  case class ConnectionState(httpOut: Option[HttpOutPMSubscriber])

}
