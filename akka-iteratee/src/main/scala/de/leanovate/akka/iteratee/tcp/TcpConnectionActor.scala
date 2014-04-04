package de.leanovate.akka.iteratee.tcp

import akka.actor.{Props, Actor, ActorRef}
import java.net.InetSocketAddress
import akka.io.Tcp
import akka.io.Tcp.ConnectionClosed

class TcpConnectionActor(connection: ActorRef, remote: InetSocketAddress, local: InetSocketAddress) extends Actor {
  import context.dispatcher

  val in = new InStreamEnumerator(connection)

  val out = new OutStreamAdapter(connection, RawWriter.raw, WriteAck)

  connection ! Tcp.Register(self)

  override def receive = {
    case Tcp.Received(data) =>
      in.feedChunk(data)

    case _: ConnectionClosed =>
      in.feedEOF()
      context stop self

    case Tcp.CommandFailed(_: Tcp.Write) =>
      in.feedError("TCP write failed")
      context stop self

    case WriteAck =>
      out.acknowledge()
  }

  private case object WriteAck extends Tcp.Event
}

object TcpConnectionActor {
  def props(connection: ActorRef, remote: InetSocketAddress, local: InetSocketAddress) =
    Props(classOf[TcpConnectionActor], connection, remote, local)
}