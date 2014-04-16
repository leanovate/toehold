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
import org.specs2.mutable.SpecificationLike
import org.specs2.matcher.ShouldMatchers
import org.specs2.mock.Mockito
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

class TcpConnectedStateSpec
  extends TestKit(ActorSystem("testSystem")) with ImplicitSender with SpecificationLike with ShouldMatchers with
  Mockito {

  val mockActor = TestActorRef[TcpConnectedStateSpec.MockActor]

  val mockConnection = TestActorRef[TcpConnectedStateSpec.MockConnection]

  val inStream = new CollectingPMStream[String]

  mockActor !
    TcpConnectedStateSpec.Connect(InetSocketAddress.createUnresolved("localhost", 1234),
                                   InetSocketAddress.createUnresolved("localhost", 4321),
                                   mockConnection, PMPipe.map[ByteString, String](_.utf8String) |> inStream,
                                   closeOnEof = true)

  val outStream = receiveOne(Duration(1, SECONDS)).asInstanceOf[PMStream[ByteString]]

  sequential

  "TcpConnectedState" should {
    "immediatly suspend reading on tcp receive and resume one instream resumes" in {
      inStream.clear()

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() shouldEqual Seq("something in")
      inStream.markResume()

      assertTcpMessages(Tcp.ResumeReading)
    }

    "abort the tcp connection if stream aborts" in {
      inStream.clear()

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() shouldEqual Seq("something in")
      inStream.markAbort("Some reason")

      assertTcpMessages(Tcp.Abort)
    }

    "should resume out stream on WriteAck" in {
      val ctrl = mock[Control]

      outStream.send(Data(ByteString("something out")), ctrl)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      there was noCallsTo(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      there was one(ctrl).resume()
    }
  }

  step {
    TestKit.shutdownActorSystem(system)
  }

  def assertTcpMessages(msgs: Any*) = {

    msgs.foreach {
      msg =>
        mockConnection.underlyingActor.msgs.dequeue() shouldEqual msg
    }
    mockConnection.underlyingActor.msgs should have size 0
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