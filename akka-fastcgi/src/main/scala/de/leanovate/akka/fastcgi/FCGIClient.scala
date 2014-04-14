package de.leanovate.akka.fastcgi

import akka.actor.{Props, Actor}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import de.leanovate.akka.fastcgi.records.{FilterStdOut, BytesToFCGIRecords, FCGIRecord}
import de.leanovate.akka.tcp.{AttachablePMStream, PMPipe, TcpConnectionActor}
import akka.util.ByteString

class FCGIClient(remote: InetSocketAddress, handler: FCGIConnectionHandler) extends Actor with TcpConnectionActor {

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
      sender ! Register(self)
      val in = new AttachablePMStream[ByteString]
      val httpExtractor = new HeaderExtractor({
        (statusCode, statusLine, headers) =>
          handler.headerReceived(statusCode, statusLine, headers, in)
      })
      val pipeline =
        PMPipe.flatMap(new BytesToFCGIRecords) |>
          PMPipe.flatMap(new FilterStdOut(stderrToLog)) |>
          PMPipe.flatMap(httpExtractor) |> in
      val outStream = becomeConnected(remoteAddress, localAddress, sender, pipeline, closeOnEof = false)
      handler.connected(PMPipe.map[FCGIRecord, ByteString](_.encode) |> outStream)
  }

  private def stderrToLog(stderr: ByteString) {

    log.error(s"Stderr: ${stderr.utf8String}")
  }

}

object FCGIClient {
  def props(hostname: String, port: Int, handler: FCGIConnectionHandler) =
    Props(classOf[FCGIClient], new InetSocketAddress(hostname, port), handler)

  def props(remote: InetSocketAddress, handler: FCGIConnectionHandler) =
    Props(classOf[FCGIClient], remote, handler)

}