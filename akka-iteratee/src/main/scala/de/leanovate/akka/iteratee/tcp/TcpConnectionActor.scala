package de.leanovate.akka.iteratee.tcp

import akka.actor.{Props, Actor, ActorRef}
import java.net.InetSocketAddress
import akka.io.Tcp
import akka.io.Tcp.{ResumeReading, ConnectionClosed}
import akka.io.Udp.SuspendReading

class TcpConnectionActor(connection: ActorRef, remote: InetSocketAddress, local: InetSocketAddress) extends Actor {

  import context.dispatcher

  val in = new InStreamEnumerator(connection)

  val out = new OutStreamAdapter(connection, RawWriter.raw, WriteAck, closeOnEof = true)

  connection ! Tcp.Register(self)

  override def receive = {

    case Tcp.Received(data) =>
      connection ! SuspendReading
      in.sendChunk(data, resume)

    case _: ConnectionClosed =>
      in.sendEOF()
      context stop self

    case Tcp.CommandFailed(_: Tcp.Write) =>
      context stop self

    case WriteAck =>
      out.acknowledge()
  }

  private def resume() {

    connection ! ResumeReading
  }

  private case object WriteAck extends Tcp.Event

}

object TcpConnectionActor {
  def props(connection: ActorRef, remote: InetSocketAddress, local: InetSocketAddress) =
    Props(classOf[TcpConnectionActor], connection, remote, local)
}