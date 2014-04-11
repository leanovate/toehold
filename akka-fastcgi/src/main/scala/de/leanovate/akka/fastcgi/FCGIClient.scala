package de.leanovate.akka.fastcgi

import akka.actor.{ActorLogging, Props, Actor}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import de.leanovate.akka.fastcgi.records.{FilterStdOut, BytesToFCGIRecords, FCGIRecord}
import de.leanovate.akka.iteratee.tcp.{TcpConnected, InStreamEnumerator, OutStreamAdapter}
import akka.util.ByteString

class FCGIClient(remote: InetSocketAddress, handler: FCGIConnectionHandler) extends Actor with ActorLogging {

  import context.system
  import context.dispatcher

  IO(Tcp) ! Connect(remote)

  def receive = {

    case c@CommandFailed(_: Connect) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connect failed: $c")
      }
      handler.connectionFailed()
    case c@Connected(remoteAddress, localAddress) =>
      if (log.isDebugEnabled) {
        log.debug(s"Connected $remoteAddress -> $localAddress")
      }
      sender ! Register(self)
      val in = new InStreamEnumerator(sender)
      val httpExtractor = new HeaderExtractor({
        (statusCode, statusLine, headers) =>
          handler.headerReceived(statusCode, statusLine, headers, in)
      }, in)
      val filterStdOut = new FilterStdOut(stderrToLog, httpExtractor)
      val bytesToFCGIRecords = new BytesToFCGIRecords(filterStdOut)
      val connected = new TcpConnected(sender, bytesToFCGIRecords, closeOnEof = false)
      val out = new OutStreamAdapter[FCGIRecord](connected.outStream, FCGIRecord)
      context become connected.state
      handler.connected(out.iterator)
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