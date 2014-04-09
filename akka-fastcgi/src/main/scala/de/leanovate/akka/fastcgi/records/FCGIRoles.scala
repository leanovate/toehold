/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.records

object FCGIRoles extends Enumeration {
  type Type = Value

  val FCGI_RESPONDER = Value(1)

  val FCGI_AUTHORIZER = Value(2)

  val FCGI_FILTER = Value(3)
}
