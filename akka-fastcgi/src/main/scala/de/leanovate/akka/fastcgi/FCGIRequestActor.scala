/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import akka.actor._
import de.leanovate.akka.fastcgi.request._
import de.leanovate.akka.fastcgi.request.FCGIResponderSuccess
import akka.actor.Terminated
import scala.concurrent.duration.FiniteDuration
import de.leanovate.akka.pool.PoolSupport

class FCGIRequestActor(host: String, port: Int, inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration,
                       maxConnections: Int) extends Actor with PoolSupport[FCGIRequest] with ActorLogging {

  var count = 0

  context.system.eventStream.subscribe(self, classOf[FCGIResponderSuccess])

  initializePool(maxConnections)

  override def receive = handlePool orElse {

    case request: FCGIRequest =>
      poolRequest(request)

    case FCGIQueryStatus =>
      sender ! FCGIStatus(busyPool.size, idlePool.size, freePool.size)

    case deadResponse: FCGIResponderSuccess =>
      deadResponse.content.abort("Response went to deadLetter (most likely due to timeout)")

    case Terminated(client) =>
      idlePool.dequeueAll(_ == client)
      freePool.dequeueAll(_ == client)
      busyPool.remove(client)
      if (log.isDebugEnabled) {
        log.debug(s"FCGIClient terminated $client")
      }
      freePool.enqueue(createPoolable())
  }

  override def createPoolable() = {

    val client = context.actorOf(FCGIClient.props(host, port, inactivityTimeout, suspendTimeout), s"FCGIClient$count")
    if (log.isDebugEnabled) {
      log.debug(s"Create FCGIClient $client")
    }
    count += 1
    context.watch(client)
    client
  }
}

object FCGIRequestActor {
  def props(host: String, port: Int, inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration, maxConnections: Int) =
    Props(classOf[FCGIRequestActor], host, port, inactivityTimeout, suspendTimeout, maxConnections)
}
