package de.leanovate.akka.fastcgi.records

object FCGIRoles extends Enumeration {
  type Type = Value

  val FCGI_RESPONDER = Value(1)

  val FCGI_AUTHORIZER = Value(2)

  val FCGI_FILTER = Value(3)
}
