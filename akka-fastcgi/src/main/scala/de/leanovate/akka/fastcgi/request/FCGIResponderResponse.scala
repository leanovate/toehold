package de.leanovate.akka.fastcgi.request

import akka.util.ByteString
import play.api.libs.iteratee.Enumerator

case class FCGIResponderResponse(headers:Seq[(String, String)], content: Enumerator[ByteString]) {

}
