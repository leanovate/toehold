package de.leanovate.akka.fastcgi

import scala.concurrent.duration.FiniteDuration
import akka.actor.{Terminated, Props, ActorLogging, Actor}
import de.leanovate.akka.fastcgi.request.FCGIRequestWithRemote

class SimpleFCGIRequestActor(inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration) extends Actor with ActorLogging {
  var count = 0

  override def receive = {
    case request: FCGIRequestWithRemote =>
      val client = createClient()

      client.tell(request, sender)

    case Terminated(client) =>
      if (log.isDebugEnabled) {
        log.debug(s"FCGIClient terminated $client")
      }
  }

  private def createClient() = {

    val client = context.actorOf(SimpleFCGIClient.props(inactivityTimeout, suspendTimeout), s"SimpleFCGIClient$count")
    if (log.isDebugEnabled) {
      log.debug(s"Create FCGIClient $client")
    }
    count += 1
    context.watch(client)
    client
  }
}

object SimpleFCGIRequestActor {
  def props(inactivityTimeout: FiniteDuration, suspendTimeout: FiniteDuration) =
    Props(classOf[SimpleFCGIRequestActor], inactivityTimeout, suspendTimeout)
}