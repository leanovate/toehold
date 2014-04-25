/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.util.ByteString
import akka.io.Tcp
import akka.io.Tcp.{Event, ConnectionClosed}
import de.leanovate.akka.tcp.PMSubscriber._
import scala.concurrent.stm._
import java.net.InetSocketAddress
import scala.concurrent.duration._
import akka.io.Tcp.Register
import de.leanovate.akka.tcp.PMSubscriber.Data
import scala.Some
import akka.io.Tcp.Received

/**
 * Helper for the connected state of an actor doing some sort of tcp communication.
 *
 * All the back-pressure handling happens here.
 */
trait TcpConnectedSupport extends TickSupport with ActorLogging {
  actor: Actor =>

  def inactivityTimeout: FiniteDuration

  def suspendTimeout: FiniteDuration

  def becomeDisconnected()

  var connectedState: Option[TcpConnectedState] = None

  def becomeConnected(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
                      connection: ActorRef, inStream: PMSubscriber[ByteString], closeOnEof: Boolean): PMSubscriber[ByteString] = {

    val (connected, outPMStream) = connectedState(remoteAddress, localAddress, connection, inStream, closeOnEof)

    context become connected
    outPMStream
  }

  def connectedState(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress,
                     connection: ActorRef, inStream: PMSubscriber[ByteString],
                     closeOnEof: Boolean): (Actor.Receive, PMSubscriber[ByteString]) = {

    def onDisconnected() {
      connectedState = None
      becomeDisconnected()
    }

    val state = new TcpConnectedState(connection, remoteAddress, localAddress,
      inStream, onDisconnected, closeOnEof, inactivityTimeout, suspendTimeout, log)
    connectedState = Some(state)

    (state.receive, state.outStream)
  }
}

object TcpConnectedSupport {

  private[tcp] case object WriteAck extends Event

}