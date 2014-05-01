package de.leanovate.akka.tcp

import de.leanovate.akka.tcp.PMSubscriber.{Subscription, EOF, Data}

class StreamPMPublisher[A](stream: Stream[A]) extends PMPublisher[A] {
  override def subscribe(consumer: PMSubscriber[A]) {
    val iterator = stream.iterator
    var eof = false

    def feed() {
      if (!eof) {
        val chunk = if (iterator.hasNext)
          Data(iterator.next())
        else {
          eof = true
          EOF
        }
        consumer.onNext(chunk)
      }
    }

    consumer.onSubscribe(new Subscription {
      override def requestMore() {
        feed()
      }

      override def cancel(msg: String) {
      }
    })

    feed()
  }

  override def abort(msg: String) {
  }
}
