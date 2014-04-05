package de.leanovate.akka.fastcgi.request

import akka.util.ByteString
import play.api.libs.iteratee.Enumerator

sealed trait FCGIResponderResponse

case class FCGIResponderSuccess(headers: Seq[(String, String)], content: Enumerator[ByteString])
  extends FCGIResponderResponse

case class FCGIResponderError(msg: String) extends FCGIResponderResponse
