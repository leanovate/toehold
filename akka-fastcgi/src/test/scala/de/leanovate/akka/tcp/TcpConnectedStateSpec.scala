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
import de.leanovate.akka.testutil.CollectingPMConsumer
import scala.collection.mutable
import akka.io.Tcp
import de.leanovate.akka.tcp.PMConsumer.{EOF, Data, Subscription}
import de.leanovate.akka.tcp.TcpConnectedState.WriteAck
import org.specs2.mutable.{Specification, After}
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

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

      val subscription = mock[Subscription]

      outStream.onSubscribe(subscription)
      outStream.onNext(Data(ByteString("something out")))

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(subscription)

      mockActor ! TcpConnectedState.WriteAck

      there was one(subscription).resume()
    }

    "buffer all output between Write and WriteAck" in new ConnectedMockActor {
      override def closeOnEof = true

      val subscription = mock[Subscription]

      outStream.onSubscribe(subscription)
      outStream.onNext(Data(ByteString("something out")))

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(subscription)

      outStream.onNext(Data(ByteString("some more out")))
      assertTcpMessages()
      there was noCallsTo(subscription)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages(Tcp.Write(ByteString("some more out"), WriteAck))
      there was noCallsTo(subscription)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages()
      there was one(subscription).resume()
    }

    "close connection immediately if buffer is empty and closeOfRef is true" in new ConnectedMockActor {

      override def closeOnEof = true

      val subscription = mock[Subscription]

      outStream.onSubscribe(subscription)
      outStream.onNext(EOF)

      assertTcpMessages(Tcp.Close)
      there was noCallsTo(subscription)
    }

    "close connection if closeOnEof is true and EOF is send to outstream" in new ConnectedMockActor {
      override def closeOnEof = true

      val subscription = mock[Subscription]

      outStream.onSubscribe(subscription)
      outStream.onNext(Data(ByteString("something out")))
      outStream.onNext(EOF)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(subscription)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages(Tcp.Close)
      there was noCallsTo(subscription)
    }

    "not close connection if closeOnEof is false and EOF is send to outstream" in new ConnectedMockActor {
      override def closeOnEof = false

      val subscription = mock[Subscription]

      outStream.onSubscribe(subscription)
      outStream.onNext(Data(ByteString("something out")))
      outStream.onNext(EOF)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(subscription)

      mockActor ! TcpConnectedState.WriteAck

      assertTcpMessages()
      there was noCallsTo(subscription)
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
        .stripMargin))

    val sender = TestProbe()

    implicit val self = sender.ref

    def closeOnEof: Boolean

    val mockActor = TestActorRef[TcpConnectedStateSpec.MockActor]

    val mockConnection = TestActorRef[TcpConnectedStateSpec.MockConnection]

    val inStream = new CollectingPMConsumer[String]

    mockActor !
      TcpConnectedStateSpec.Connect(InetSocketAddress.createUnresolved("localhost", 1234),
                                     InetSocketAddress.createUnresolved("localhost", 4321),
                                     mockConnection, PMProcessor.map[ByteString, String](_.utf8String) |> inStream,
                                     closeOnEof)

    val outStream = sender.receiveOne(Duration(1, SECONDS)).asInstanceOf[PMConsumer[ByteString]]

    assertTcpMessages(Tcp.Register(mockActor))

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
    inStream: PMConsumer[ByteString], closeOnEof: Boolean)

  class MockActor extends Actor with TcpConnectedState {
    override def inactivityTimeout = 60.seconds

    override def suspendTimeout = 20.seconds

    override def receive = {

      case Connect(remote, local, connection, inStream, closeOnEof) =>
        sender ! becomeConnected(remote, local, connection, inStream, closeOnEof)
    }

    override def becomeDisconnected() {

      context stop self
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