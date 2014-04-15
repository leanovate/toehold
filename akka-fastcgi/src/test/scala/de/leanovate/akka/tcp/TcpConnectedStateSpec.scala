package de.leanovate.akka.tcp

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{FunSpecLike, BeforeAndAfterAll, Matchers}
import java.net.InetSocketAddress
import akka.util.ByteString
import de.leanovate.akka.testutil.CollectingPMStream
import scala.collection.mutable
import scala.concurrent.duration._
import akka.io.Tcp

class TcpConnectedStateSpec
  extends TestKit(ActorSystem("testSystem")) with ImplicitSender with FunSpecLike with Matchers with BeforeAndAfterAll {

  val mockActor = TestActorRef[TcpConnectedStateSpec.MockActor]

  val mockConnection = TestActorRef[TcpConnectedStateSpec.MockConnection]

  val inStream = new CollectingPMStream[String]

  var outStream: PMStream[ByteString] = _

  override def afterAll {

    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll {

    mockActor !
      (InetSocketAddress.createUnresolved("localhost", 1234), InetSocketAddress.createUnresolved("localhost", 4321),
        mockConnection, PMPipe.map[ByteString, String](_.utf8String) |> inStream, true)
    outStream = receiveOne(1 second).asInstanceOf[PMStream[ByteString]]
  }

  describe("TcpConnectedState") {
    it("should immediatly suspend reading on tcp receive and resume one instream resumes") {
      inStream.clear()

      mockActor ! Tcp.Received(ByteString("something"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() should be(Seq("something"))
      inStream.markResume()

      assertTcpMessages(Tcp.ResumeReading)
    }

    it("should abort the tcp connection if stream aborts") {
      inStream.clear()

      mockActor ! Tcp.Received(ByteString("something"))

      assertTcpMessages(Tcp.SuspendReading)
      inStream.result() should be(Seq("something"))
      inStream.markAbort("Some reason")

      assertTcpMessages(Tcp.Abort)
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

  class MockActor extends Actor with TcpConnectedState {
    override def receive = {

      case (remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef, inStream: PMStream[ByteString], closeOnEof: Boolean) =>
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