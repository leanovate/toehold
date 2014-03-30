package de.leanovate.akka.fastcgi

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import de.leanovate.akka.fastcgi.records.{BytesToFCGIRecords, FCGIRecord}
import FCGIClient._
import de.leanovate.akka.fastcgi.iteratee.{InStreamEnumerator, OutStreamAdapter}

class FCGIClient(remote: InetSocketAddress, handler: FCGIConnectionHandler) extends Actor with ActorLogging {

  import context.system
  import context.dispatcher

  IO(Tcp) ! Connect(remote)

  def receive = {

    case c@CommandFailed(_: Connect) =>
      handler.connectionFailed()
    case c@Connected(remote, local) =>
      sender ! Register(self)
      val out = new OutStreamAdapter[FCGIRecord](sender, FCGIRecord, SendRecordAck)
      val in = new InStreamEnumerator(sender)
      context become connected(sender, in, out)
      handler.connected(in &> BytesToFCGIRecords.enumeratee, out.iterator)
  }

  def connected(connection: ActorRef, in: InStreamEnumerator,
    out: OutStreamAdapter[FCGIRecord]): PartialFunction[Any, Unit] = {

    case Received(data) =>
      in.feedChunk(data)
    case SendRecordAck =>
      out.acknowledge()
    case _: ConnectionClosed =>
      in.feedEOF()
      println(">>> Close")
      context stop self
  }
}

object FCGIClient {
  def props(hostname: String, port: Int, handler: FCGIConnectionHandler) =
    Props(classOf[FCGIClient], new InetSocketAddress(hostname, port), handler)

  def props(remote: InetSocketAddress, handler: FCGIConnectionHandler) =
    Props(classOf[FCGIClient], remote, handler)

  case object SendRecordAck extends Event

}