/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.pool

import akka.actor.{ActorRef, Actor}
import scala.collection.mutable

trait PoolSupport[R] {
  actor: Actor =>

  val freePool = mutable.Queue.empty[ActorRef]
  val idlePool = mutable.Queue.empty[ActorRef]
  val busyPool = mutable.Set.empty[ActorRef]

  val pendingRequests = mutable.Queue.empty[(R, ActorRef)]

  def handlePool: Actor.Receive = {
    case PoolSupport.IamBusy =>
      freePool.dequeueAll(_ == sender)
      idlePool.dequeueAll(_ == sender)
      busyPool.add(sender)

    case PoolSupport.IamIdle =>
      freePool.dequeueAll(_ == sender)
      busyPool.remove(sender)
      if (pendingRequests.isEmpty) {
        if (!idlePool.exists(_ == sender))
          idlePool.enqueue(sender)
      } else {
        val (request, target) = pendingRequests.dequeue()
        idlePool.dequeueAll(_ == sender)
        sender.tell(request, target)
      }

    case PoolSupport.IamFree =>
      idlePool.dequeueAll(_ == sender)
      busyPool.remove(sender)
      if (pendingRequests.isEmpty) {
        if (!freePool.exists(_ == sender))
          freePool.enqueue(sender)
      } else {
        val (request, target) = pendingRequests.dequeue()
        freePool.dequeueAll(_ == sender)
        sender.tell(request, target)
      }
  }

  def poolRequest(request: R) {
    if (!idlePool.isEmpty)
      idlePool.dequeue().tell(request, sender)
    else if (!freePool.isEmpty)
      freePool.dequeue().tell(request, sender)
    else
      pendingRequests.enqueue((request, sender))
  }

  def initializePool(maxSize: Int) = {
    Range(0, maxSize).foreach {
      _ =>
        freePool.enqueue(createPoolable())
    }
  }

  def createPoolable(): ActorRef
}

object PoolSupport {

  sealed trait StateChange

  case object IamBusy extends StateChange

  case object IamIdle extends StateChange

  case object IamFree extends StateChange

}