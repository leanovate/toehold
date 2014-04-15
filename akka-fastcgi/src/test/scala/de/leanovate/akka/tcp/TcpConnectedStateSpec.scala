package de.leanovate.akka.tcp

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{FunSpecLike, BeforeAndAfterAll, Matchers}
import java.net.InetSocketAddress
import akka.util.ByteString
import de.leanovate.akka.testutil.{RealMockitoSugar, CollectingPMStream}
import scala.collection.mutable
import scala.concurrent.duration._
import akka.io.Tcp
import de.leanovate.akka.tcp.PMStream.{Data, Control}
import de.leanovate.akka.tcp.TcpConnectedState.WriteAck
import org.mockito.Mockito.{verify, verifyZeroInteractions}

class TcpConnectedStateSpec
  extends TestKit(ActorSystem("testSystem")) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfterAll with RealMockitoSugar{

  val mockActor = TestActorRef[TcpConnectedStateSpec.MockActor]

  val mockConnection = TestActorRef[TcpConnectedStateSpec.MockConnection]

  val inStream = new CollectingPMStream[String]

  var outStream: PMStream[ByteString] = _

  override def afterAll {

    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll {

    mockActor !
      TcpConnectedStateSpec.Connect(InetSocketAddress.createUnresolved("localhost", 1234),
                                     InetSocketAddress.createUnresolved("localhost", 4321),
                                     mockConnection, PMPipe.map[ByteString, String](_.utf8String) |> inStream,
                                     closeOnEof = true)
    outStream = receiveOne(1 second).asInstanceOf[PMStream[ByteString]]
  }

  describe("TcpConnectedState") {
    it("should immediatly suspend reading on tcp receive and resume one instream resumes") {
      inStream.clear()

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() should be(Seq("something in"))
      inStream.markResume()

      assertTcpMessages(Tcp.ResumeReading)
    }

    it("should abort the tcp connection if stream aborts") {
      inStream.clear()

      mockActor ! Tcp.Received(ByteString("something in"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() should be(Seq("something in"))
      inStream.markAbort("Some reason")

      assertTcpMessages(Tcp.Abort)
    }

    it("should resume out stream on WriteAck") {
      val ctrl = mock[Control]

      outStream.send(Data(ByteString("something out")), ctrl)

      assertTcpMessages(Tcp.Write(ByteString("something out"), WriteAck))
      verifyZeroInteractions(ctrl)

      mockActor ! TcpConnectedState.WriteAck

      verify(ctrl).resume()
    }
  }

  def assertTcpMessages(msgs: Any*) {

    msgs.foreach {
      msg =>
        mockConnection.underlyingActor.msgs.dequeue() should be(msg)
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