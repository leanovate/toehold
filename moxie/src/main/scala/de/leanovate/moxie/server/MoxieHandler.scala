package de.leanovate.moxie.server

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import de.leanovate.akka.tcp.{PMStream, TcpConnectedState}
import java.net.InetSocketAddress
import de.leanovate.moxie.framing.Framing
import play.api.libs.json.JsValue
import scala.concurrent.duration._

class MoxieHandler(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress, connection: ActorRef)
  extends Actor with ActorLogging with TcpConnectedState {

  val idleTimeout = 20.seconds

  val inStream = Framing.zeroTerminatedString |> Framing.bytesToJsValue |> PMStream.nullStream[JsValue]

  val (tcpHandler, outStream) = connectedState(remoteAddress, localAddress, connection, inStream, closeOnEof = true)

  override def receive = tcpHandler
}

object MoxieHandler {
  def props(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress, connection: ActorRef) =
    Props(classOf[MoxieHandler], remoteAddress, localAddress, connection)
}