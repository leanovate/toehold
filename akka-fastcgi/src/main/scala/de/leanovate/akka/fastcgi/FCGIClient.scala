package de.leanovate.akka.fastcgi

import akka.actor.{Props, Actor}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import de.leanovate.akka.fastcgi.records.FCGIRecord
import de.leanovate.akka.tcp.{AttachablePMStream, PMPipe, TcpConnectedState}
import akka.util.ByteString
import de.leanovate.akka.fastcgi.framing.{Framing, HeaderExtractor, BytesToFCGIRecords, FilterStdOut}
import scala.concurrent.duration.FiniteDuration

class FCGIClient(remote: InetSocketAddress, val idleTimeout: FiniteDuration, handler: FCGIConnectionHandler)
  extends Actor with TcpConnectedState {

  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {

    case c@CommandFailed(_: Connect) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connect failed: $c")
      }
      handler.connectionFailed()
    case c@Connected(remoteAddress, localAddress) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connected $localAddress -> $remoteAddress")
      }
      val in = new AttachablePMStream[ByteString]
      val httpExtractor = new HeaderExtractor({
        (statusCode, statusLine, headers) =>
          handler.headerReceived(statusCode, statusLine, headers, in)
      })
      val pipeline =
        Framing.bytesToFCGIRecords |>
          Framing.filterStdOut(stderrToLog) |>
          PMPipe.flatMapChunk(httpExtractor) |> in
      val outStream = becomeConnected(remoteAddress, localAddress, sender, pipeline, closeOnEof = false)
      handler.connected(PMPipe.map[FCGIRecord, ByteString](_.encode) |> outStream)
  }

  override def becomeDisconnected() {
    context stop self
  }

  private def stderrToLog(stderr: ByteString) {

    log.error(s"Stderr: ${stderr.utf8String}")
  }

}

object FCGIClient {
  def props(hostname: String, port: Int, idleTimeout: FiniteDuration, handler: FCGIConnectionHandler) =
    Props(classOf[FCGIClient], new InetSocketAddress(hostname, port), idleTimeout, handler)

  def props(remote: InetSocketAddress, idleTimeout: FiniteDuration, handler: FCGIConnectionHandler) =
    Props(classOf[FCGIClient], remote, idleTimeout, handler)
}