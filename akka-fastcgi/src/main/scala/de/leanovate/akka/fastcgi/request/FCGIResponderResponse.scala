/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.request

import akka.util.ByteString
import de.leanovate.akka.tcp.AttachablePMSubscriber

sealed trait FCGIResponderResponse

case class FCGIResponderSuccess(statusCode: Int, statusLine: String, headers: Seq[(String, String)],
  content: AttachablePMSubscriber[ByteString])
  extends FCGIResponderResponse

case class FCGIResponderError(msg: String) extends FCGIResponderResponse
