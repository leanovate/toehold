package de.leanovate.akka.fastcgi

import akka.actor.ActorSystem
import play.api.libs.iteratee.{Enumerator, Iteratee}
import de.leanovate.akka.fastcgi.records._
import akka.util.ByteString
import de.leanovate.akka.fastcgi.records.FCGIParams
import de.leanovate.akka.fastcgi.records.FCGIBeginRequest
import de.leanovate.akka.fastcgi.request.FCGIResponderRequest
import java.net.URI

object Main {
  def main(args: Array[String]) {

    implicit val system = ActorSystem()

    import system.dispatcher

    val handler = new FCGIConnectionHandler {
      override def connectionFailed() = {

        println(">>> Connection failed")
      }

      override def connected(in: Enumerator[FCGIRecord], out: Iteratee[FCGIRecord, Unit]) = {

        println(">>> Connected")

        in |>> Iteratee.foreach[FCGIRecord] {
          case FCGIStdErr(_, content) =>
            println("Err: " + content.utf8String)
          case FCGIStdOut(_, content) =>
            println("Out: " + content.utf8String)
          case record =>
            println(record)
        }.map {
          _ =>
            println("EOF")
        }
        val request = FCGIResponderRequest("GET", "/test.php", "", "./app-php",
                                            Map.empty,
                                            Enumerator.eof)

        request.records(1) |>> out
      }
    }

    val server = system.actorOf(FCGIClient.props("localhost", 9110, handler))

    Thread.sleep(5000L)
  }
}
