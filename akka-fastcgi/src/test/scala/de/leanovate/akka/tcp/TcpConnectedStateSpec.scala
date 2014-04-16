package de.leanovate.akka.tcp

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import java.net.InetSocketAddress
import akka.util.ByteString
import de.leanovate.akka.testutil.CollectingPMStream
import scala.collection.mutable
import akka.io.Tcp
import de.leanovate.akka.tcp.PMStream.{Data, Control}
import de.leanovate.akka.tcp.TcpConnectedState.WriteAck
import org.specs2.mutable.{After, SpecificationLike}
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

class TcpConnectedStateSpec
  extends TestKit(ActorSystem("testSystem")) with ImplicitSender with SpecificationLike with ShouldMatchers with
  Mockito {
  isolated

  trait ConnectedMockActor extends After {
    val closeOnEof: Boolean

    val mockActor = TestActorRef[TcpConnectedStateSpec.MockActor]

    val mockConnection = TestActorRef[TcpConnectedStateSpec.MockConnection]

    val inStream = new CollectingPMStream[String]

    mockActor !
      TcpConnectedStateSpec.Connect(InetSocketAddress.createUnresolved("localhost", 1234),
                                     InetSocketAddress.createUnresolved("localhost", 4321),
                                     mockConnection, PMPipe.map[ByteString, String](_.utf8String) |> inStream,
                                     closeOnEof)

    val outStream = receiveOne(Duration(1, SECONDS)).asInstanceOf[PMStream[ByteString]]

    def assertTcpMessages(msgs: Any*) = {

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

  "TcpConnectedState" should {
    "immediatly suspend reading on tcp receive and resume one instream resumes" in new ConnectedMockActor {
      override val closeOnEof = true

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() shouldEqual Seq("something in")
      inStream.markResume()

      assertTcpMessages(Tcp.ResumeReading)
    }

    "abort the tcp connection if stream aborts" in new ConnectedMockActor {
      override val closeOnEof = true

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() shouldEqual Seq("something in")
      inStream.markAbort("Some reason")

      assertTcpMessages(Tcp.Abort)
    }

    "resume out stream on WriteAck" in new ConnectedMockActor {
      override val closeOnEof = true

      val ctrl = mock[Control]

      outStream.send(Data(ByteString("something out")), ctrl)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      there was one(ctrl).resume()
    }

    "buffer all output between Write and WriteAck" in new ConnectedMockActor {
      override val closeOnEof = true

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