package de.leanovate.akka.fastcgi.request

import de.leanovate.akka.tcp.PMSubscriber
import de.leanovate.akka.fastcgi.records.FCGIRecord

trait FCGIRequest {
  def writeTo(id: Int, out: PMSubscriber[FCGIRecord])
}
