package de.leanovate.akka.tcp

trait PMPublisher[A] {
  def subscribe(consumer: PMSubscriber[A])
}
