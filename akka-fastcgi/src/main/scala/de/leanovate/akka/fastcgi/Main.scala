package de.leanovate.akka.fastcgi

import akka.actor.ActorSystem
import play.api.libs.iteratee.{Enumerator, Iteratee}
import de.leanovate.akka.fastcgi.records._
import akka.util.ByteString
import de.leanovate.akka.fastcgi.records.FCGIParams
import de.leanovate.akka.fastcgi.records.FCGIBeginRequest

object Main {
  def main(args: Array[String]) {

    implicit val system = ActorSystem()

    import system.dispatcher

    val handler = new FCGIConnectionHandler {
      override def connectionFailed() = {

        println(">>> Connection failed")
      }

      override def connected(in: Enumerator[ByteString], out: Iteratee[FCGIRecord, Unit]) = {

        println(">>> Connected")

        in |>> Iteratee.foreach[ByteString] {
          chunk =>
            println(chunk.utf8String)
        }
        val env = Seq(
                       "SCRIPT_FILENAME" -> "/vagrant/test.php",
                       "QUERY_STRING" -> "",
                       "REQUEST_METHOD" -> "GET",
                       "SCRIPT_NAME" -> "/test.php",
                       "REQUEST_URI" -> "/test.php",
                       "DOCUMENT_URI" -> "/test.php",
                       "DOCUMENT_ROOT" -> "/vagrant",
                       "SERVER_PROTOCOL" -> "HTTP/1.1",
                       "GATEWAY_INTERFACE" -> "CGI/1.1"
                     ).map(e => ByteString(e._1) -> ByteString(e._2))
        Enumerator[FCGIRecord](
                                FCGIBeginRequest(1, FCGIRoles.FCGI_RESPONDER, keepAlive = false),
                                FCGIParams(2, env),
                                FCGIParams(3, Seq.empty),
                                FCGIStdin(4, ByteString())
                              ) |>> out
      }
    }

    val server = system.actorOf(FCGIClient.props("localhost", 9111, handler))

    Thread.sleep(5000L)
  }
}
