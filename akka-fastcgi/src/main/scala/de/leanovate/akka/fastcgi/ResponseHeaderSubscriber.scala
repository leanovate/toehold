/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi

import de.leanovate.akka.tcp.PMSubscriber
import akka.util.ByteString
import de.leanovate.akka.tcp.PMSubscriber._
import de.leanovate.akka.tcp.PMSubscriber.Data

abstract class ResponseHeaderSubscriber extends PMSubscriber[ByteString] {
  private var subscription: Subscription = NoSubscription

  private var state: (Chunk[ByteString] => Unit) = parseState

  var buffer = ByteString.empty

  val extractedHeaders = Seq.newBuilder[(String, String)]

  var extractedStatusCode = 200

  var extractedStatusLine = "OK"

  def onHeader(statusCode: Int, statusLine: String, headers: Seq[(String, String)]): PMSubscriber[ByteString]

  override def onSubscribe(_subscription: Subscription) {
    subscription = _subscription
  }

  override def onNext(chunk: Chunk[ByteString]) = state(chunk)

  def parseState(chunk: Chunk[ByteString]) {
    chunk match {
      case Data(data) =>
        buffer ++= data
        var idx = buffer.indexOf('\n')
        var done = false

        while (idx >= 0 && !done) {
          val line = if (idx > 0 && buffer(idx - 1) == '\r') {
            buffer.take(idx - 1)
          } else {
            buffer.take(idx)
          }
          buffer = buffer.drop(idx + 1)
          idx = buffer.indexOf('\n')
          if (line.isEmpty) {
            val bodySubscriber = onHeader(extractedStatusCode, extractedStatusLine, extractedHeaders.result())
            bodySubscriber.onSubscribe(subscription)
            if (!buffer.isEmpty) {
              bodySubscriber.onNext(Data(buffer))
              buffer = ByteString.empty
            }
            state = bodySubscriber.onNext
            done = true
          } else {
            parseLine(line)
          }
        }
        subscription.requestMore()
      case EOF =>
        val bodySubscriber = onHeader(extractedStatusCode, extractedStatusLine, extractedHeaders.result())
        bodySubscriber.onSubscribe(subscription)
        state = bodySubscriber.onNext
        bodySubscriber.onNext(EOF)
    }

  }

  private def parseLine(line: ByteString) = {

    val delimIdx = line.indexOf(':')
    if (delimIdx >= 0) {
      val name = line.take(delimIdx).utf8String
      val value = line.drop(delimIdx + 1).utf8String.trim

      if (name.equalsIgnoreCase("status")) {
        val splitIdx = value.indexOf(' ')
        if (splitIdx < 0) {
          extractedStatusCode = value.toInt
        } else {
          extractedStatusCode = value.substring(0, splitIdx).trim.toInt
          extractedStatusLine = value.substring(splitIdx + 1).trim
        }
      } else {
        extractedHeaders += name -> value
      }
    }
  }

}
