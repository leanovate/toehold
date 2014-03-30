package de.leanovate.akka.fastcgi.records

object FCGIConstants {
  val FCGI_BEGIN_REQUEST = 1.toByte
  val FCGI_END_REQUEST = 3.toByte
  val FCGI_PARAMS = 4.toByte
  val FCGI_STDIN = 5.toByte
  val FCGI_STDOUT = 6.toByte
  val FCGI_STDERR = 7.toByte
}
