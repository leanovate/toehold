package de.leanovate.akka.fastcgi.request

case class FCGIStatus(activeConnections: Int, idleConnections: Int, disconnected: Int)
