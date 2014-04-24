package de.leanovate.akka.tcp

import akka.actor.{Cancellable, Actor}
import scala.concurrent.duration._
import scala.language.postfixOps

trait TickSupport {
  actor: Actor =>

  val tickTime = 1.second

  var tickGenerator: Option[Cancellable] = None

  def scheduleTick() {

    tickGenerator.foreach(_.cancel())
    tickGenerator = Some(context.system.scheduler
      .scheduleOnce(tickTime, self, TickSupport.Tick)(context.dispatcher))
  }

}

object TickSupport {

  case object Tick

}