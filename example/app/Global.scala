import akka.actor.PoisonPill
import de.leanovate.akka.fastcgi.FCGIRequestActor
import play.api.Play._
import play.api.{Application, GlobalSettings}
import de.leanovate.play.fastcgi.FastCGISupport

import play.api.libs.concurrent.Akka

object Global extends GlobalSettings with FastCGISupport {
}
