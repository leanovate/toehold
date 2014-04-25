/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import akka.actor._
import de.leanovate.akka.fastcgi.request._
import de.leanovate.akka.fastcgi.request.FCGIResponderSuccess
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import akka.actor.Terminated
import scala.concurrent.duration.FiniteDuration
import scala.collection.mutable

class FCGIRequestActor(host: String, port: Int, inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration,
                       maxConnections: Int) extends Actor with ActorLogging {

  val disconnectedClients = mutable.Queue.empty[ActorRef]
  val activeClients = mutable.Set.empty[ActorRef]
  val pendingRequests = mutable.Queue.empty[(FCGIRequest, ActorRef)]

  var count = 0

  context.system.eventStream.subscribe(self, classOf[FCGIResponderSuccess])

  Range(0, maxConnections).foreach {
    _ =>
      createClient()
  }

  override def receive = {

    case FCGIQueryStatus =>
      sender ! FCGIStatus(activeClients.size, 0, disconnectedClients.size)

    case request: FCGIResponderRequest =>
      if (disconnectedClients.isEmpty)
        pendingRequests.enqueue((request, sender))
      else
        disconnectedClients.dequeue().tell(request, sender)

    case deadResponse: FCGIResponderSuccess =>
      deadResponse.content.abort("Response went to deadLetter (most likely due to timeout)")

    case FCGIClient.BecomeActive =>
      activeClients.add(sender)

    case FCGIClient.BecomeDisconnected =>
      activeClients.remove(sender)
      if (pendingRequests.isEmpty)
        disconnectedClients.enqueue(sender)
      else {
        val (request, target) = pendingRequests.dequeue()
        sender.tell(request, target)
      }

    case Terminated(client) =>
      activeClients.remove(sender)
      disconnectedClients.dequeueAll(_ == client)
      if (log.isDebugEnabled) {
        log.debug(s"FCGIClient terminated $client")
      }
      createClient()
  }

  private def createClient() {

    val client = context.actorOf(FCGIClient.props(host, port, inactivityTimeout, suspendTimeout), s"FCGIClient$count")
    if (log.isDebugEnabled) {
      log.debug(s"Create FCGIClient $client")
    }
    count += 1
    context.watch(client)
    disconnectedClients.enqueue(client)
  }
}

object FCGIRequestActor {
  def props(host: String, port: Int, inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration, maxConnections: Int) =
    Props(classOf[FCGIRequestActor], host, port, inactivityTimeout, suspendTimeout, maxConnections)
}
