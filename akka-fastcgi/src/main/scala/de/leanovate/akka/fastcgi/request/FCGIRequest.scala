package de.leanovate.akka.fastcgi.request

import de.leanovate.akka.tcp.PMSubscriber
import de.leanovate.akka.fastcgi.records.FCGIRecord
import java.net.InetSocketAddress

trait FCGIRequest {
  def ref: Any

  def writeTo(id: Int, keepAlive: Boolean, out: PMSubscriber[FCGIRecord])
}

trait FCGIRequestWithRemote extends FCGIRequest {
  def remote: InetSocketAddress
}