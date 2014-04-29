package de.leanovate.akka.pool

import org.specs2.mutable.{After, Specification}
import org.specs2.matcher.ShouldMatchers
import akka.actor.{ActorSystem, Actor}
import com.typesafe.config.ConfigFactory
import akka.testkit.{TestProbe, TestActorRef, TestKit}
import scala.collection.mutable

class PoolSupportSpec extends Specification with ShouldMatchers {
  "PoolSupport" should {
    "initialize pool" in new WithMockPoolActor {
      mockPoolActor.underlyingActor.initializePool(10)

      poolables should have size 10
      mockPoolActor.underlyingActor.freePool should have size 10
    }

    "remove poolable from free on request" in new WithMockPoolActor {
      mockPoolActor.underlyingActor.initializePool(1)

      assertPoolSizes(0, 0, 1)

      mockPoolActor ! Request(1)

      poolables.head.expectMsg(Request(1))

      assertPoolSizes(0, 0, 0)

      poolables.head.send(mockPoolActor, PoolSupport.IamBusy)

      assertPoolSizes(1, 0, 0)
    }

    "remove poolable first from idle then free on request" in new WithMockPoolActor {
      mockPoolActor.underlyingActor.initializePool(2)
      poolables(0).send(mockPoolActor, PoolSupport.IamIdle)

      assertPoolSizes(0, 1, 1)

      mockPoolActor ! Request(1)
      poolables(0).expectMsg(Request(1))

      assertPoolSizes(0, 0, 1)

      mockPoolActor ! Request(2)
      poolables(1).expectMsg(Request(2))

      assertPoolSizes(0, 0, 0)

      poolables.foreach(_.send(mockPoolActor, PoolSupport.IamBusy))

      assertPoolSizes(2, 0, 0)
    }

    "queue request if free is empty" in new WithMockPoolActor {
      mockPoolActor.underlyingActor.initializePool(1)

      mockPoolActor ! Request(1)
      poolables.head.expectMsg(Request(1))

      poolables.head.send(mockPoolActor, PoolSupport.IamBusy)

      assertPoolSizes(1, 0, 0)

      mockPoolActor ! Request(2)

      poolables.head.expectNoMsg()

      poolables.head.send(mockPoolActor, PoolSupport.IamFree)

      assertPoolSizes(0, 0, 0)

      poolables.head.expectMsg(Request(2))
    }

    "queue request if idle is empty" in new WithMockPoolActor {
      mockPoolActor.underlyingActor.initializePool(1)

      mockPoolActor ! Request(1)
      poolables.head.expectMsg(Request(1))

      poolables.head.send(mockPoolActor, PoolSupport.IamBusy)

      assertPoolSizes(1, 0, 0)

      mockPoolActor ! Request(2)

      poolables.head.expectNoMsg()

      poolables.head.send(mockPoolActor, PoolSupport.IamIdle)

      assertPoolSizes(0, 0, 0)

      poolables.head.expectMsg(Request(2))
    }
  }

  trait WithMockPoolActor extends After {
    implicit val system =
      ActorSystem("TestSystem", ConfigFactory.parseString( """akka.loglevel = "DEBUG"
                                                             |akka.loggers = ["akka.testkit.TestEventListener"]"""
        .stripMargin))

    val poolables = new mutable.ArrayBuffer[TestProbe]

    val mockPoolActor = TestActorRef(new MockPoolActor)

    case class Request(id: Int)

    class MockPoolActor extends Actor with PoolSupport[Request] {
      override def receive = handlePool orElse {
        case request: Request =>
          poolRequest(request)
      }

      override def createPoolable() = {
        val probe = TestProbe()

        poolables += probe
        probe.ref
      }
    }

    override def after {

      TestKit.shutdownActorSystem(system)
    }

    def assertPoolSizes(busy: Int, idle: Int, free: Int) = {
      mockPoolActor.underlyingActor.busyPool should have size busy
      mockPoolActor.underlyingActor.idlePool should have size idle
      mockPoolActor.underlyingActor.freePool should have size free
    }
  }

}
