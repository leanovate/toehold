package de.leanovate.akka.fastcgi

import akka.actor.ActorSystem
import play.api.libs.iteratee.{Enumerator, Iteratee}
import de.leanovate.akka.fastcgi.records._
import de.leanovate.akka.fastcgi.request.{FCGIRequestContent, FCGIResponderSuccess, FCGIResponderError, FCGIResponderRequest}
import akka.util.{ByteString, Timeout}
import scala.concurrent.duration._
import akka.pattern.ask

object Main {
  def main(args: Array[String]) {

    implicit val system = ActorSystem()
    implicit val timeout = Timeout(5.seconds)

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
                                            Map.empty, None)

        request.records(1) |>> out
      }
    }

    //    val server = system.actorOf(FCGIClient.props("localhost", 9110, handler))

    val requester = system.actorOf(FCGIRequestActor.props("localhost", 9110))
    val request = FCGIResponderRequest("POST", "/test.php", "", "./app-php",
                                        Seq("Content-Type" -> Seq("text/plain")).toMap,
                                        Some(FCGIRequestContent("text/plain", "Tri tra tulla Hubba\n")))

    (requester ? request).foreach {
      case FCGIResponderSuccess(headers, content) =>
        println(headers)
        content |>> Iteratee.foreach[ByteString] {
          chunk =>
            println("Chunk: " + chunk.utf8String)
        }.map {
          _ =>
            println("EOF")
        }
      case FCGIResponderError(msg) =>
        println(s"Error: $msg")
    }

    Thread.sleep(5000L)
  }
}
