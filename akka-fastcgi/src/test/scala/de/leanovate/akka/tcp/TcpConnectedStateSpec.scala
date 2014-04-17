/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.tcp

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.testkit.{TestProbe, TestActorRef, TestKit}
import java.net.InetSocketAddress
import akka.util.ByteString
import de.leanovate.akka.testutil.CollectingPMStream
import scala.collection.mutable
import akka.io.Tcp
import de.leanovate.akka.tcp.PMStream.{EOF, Data, Control}
import de.leanovate.akka.tcp.TcpConnectedState.WriteAck
import org.specs2.mutable.{Specification, After}
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import com.typesafe.config.ConfigFactory

class TcpConnectedStateSpec extends Specification with ShouldMatchers with Mockito {

  "TcpConnectedState" should {
    "immediatly suspend reading on tcp receive and resume one instream resumes" in new ConnectedMockActor {
      override def closeOnEof = true

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() shouldEqual Seq("something in")
      inStream.markResume()

      assertTcpMessages(Tcp.ResumeReading)
    }

    "abort the tcp connection if stream aborts" in new ConnectedMockActor {
      override def closeOnEof = true

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() shouldEqual Seq("something in")
      inStream.markAbort("Some reason")

      assertTcpMessages(Tcp.Abort)
    }

    "resume out stream on WriteAck" in new ConnectedMockActor {
      override def closeOnEof = true

      val ctrl = mock[Control]

      outStream.send(Data(ByteString("something out")), ctrl)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      there was one(ctrl).resume()
    }

    "buffer all output between Write and WriteAck" in new ConnectedMockActor {
      override def closeOnEof = true

      val ctrl = mock[Control]

      outStream.send(Data(ByteString("something out")), ctrl)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(ctrl)

      outStream.send(Data(ByteString("some more out")), ctrl)
      assertTcpMessages()
      there was noCallsTo(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages(Tcp.Write(ByteString("some more out"), WriteAck))
      there was noCallsTo(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages()
      there was one(ctrl).resume()
    }

    "close connection immediately if buffer is empty and closeOfRef is true" in new ConnectedMockActor {

      override def closeOnEof = true

      val ctrl = mock[Control]

      outStream.send(EOF, ctrl)

      assertTcpMessages(Tcp.Close)
      there was noCallsTo(ctrl)
    }

    "close connection if closeOnEof is true and EOF is send to outstream" in new ConnectedMockActor {
      override def closeOnEof = true

      val ctrl = mock[Control]

      outStream.send(Data(ByteString("something out")), ctrl)
      outStream.send(EOF, ctrl)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages(Tcp.Close)
      there was noCallsTo(ctrl)
    }

    "not close connection if closeOnEof is false and EOF is send to outstream" in new ConnectedMockActor {
      override def closeOnEof = false

      val ctrl = mock[Control]

      outStream.send(Data(ByteString("something out")), ctrl)
      outStream.send(EOF, ctrl)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages()
      there was noCallsTo(ctrl)
    }

    "eof the in stream and kill itself if connection closes for some reason" in new ConnectedMockActor {
      override def closeOnEof = false

      mockActor.underlying.isTerminated should beFalse

      mockActor ! Tcp.PeerClosed

      inStream.eof should beTrue
      mockActor.underlying.isTerminated should beTrue
    }
  }

  trait ConnectedMockActor extends After {
    implicit val system =
      ActorSystem("TestSystem", ConfigFactory.parseString( """akka.loglevel = "DEBUG"
                                                             |akka.loggers = ["akka.testkit.TestEventListener"]"""
        .stripMargin)
                 )

    val sender = TestProbe()

    implicit val self = sender.ref

    def closeOnEof: Boolean

    val mockActor = TestActorRef[TcpConnectedStateSpec.MockActor]

    val mockConnection = TestActorRef[TcpConnectedStateSpec.MockConnection]

    val inStream = new CollectingPMStream[String]

    mockActor !
      TcpConnectedStateSpec.Connect(InetSocketAddress.createUnresolved("localhost", 1234),
                                     InetSocketAddress.createUnresolved("localhost", 4321),
                                     mockConnection, PMPipe.map[ByteString, String](_.utf8String) |> inStream,
                                     closeOnEof)

    val outStream = sender.receiveOne(Duration(1, SECONDS)).asInstanceOf[PMStream[ByteString]]

    def assertTcpMessages(msgs: Any*) = {

      mockConnection.underlyingActor.msgs should have size msgs.size
      msgs.foreach {
        msg =>
          mockConnection.underlyingActor.msgs.dequeue() shouldEqual msg
      }
      mockConnection.underlyingActor.msgs should have size 0
    }

    override def after {

      TestKit.shutdownActorSystem(system)
    }
  }
}

object TcpConnectedStateSpec {

  case class Connect(remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef,
    inStream: PMStream[ByteString], closeOnEof: Boolean)

  class MockActor extends Actor with TcpConnectedState {
    override def receive = {

      case Connect(remote, local, connection, inStream, closeOnEof) =>
        sender ! becomeConnected(remote, local, connection, inStream, closeOnEof)
    }
  }

  class MockConnection extends Actor {
    val msgs = mutable.Queue.empty[Any]

    override def receive = {

      case msg =>
        msgs.enqueue(msg)
    }
  }

}