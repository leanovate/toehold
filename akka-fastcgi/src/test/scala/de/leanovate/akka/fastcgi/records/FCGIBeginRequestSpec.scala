package de.leanovate.akka.fastcgi.records

import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.apache.commons.codec.binary.Hex

class FCGIBeginRequestSpec extends Specification with ShouldMatchers {
  "FCGIBeginRequest" should {
    "encode to bytestring correctly" in {
      val keepAliveRequest = FCGIBeginRequest(21, FCGIRoles.FCGI_FILTER, keepAlive = true)

      Hex.encodeHexString(keepAliveRequest.encode.toArray) shouldEqual "01010015000800000003010000000000"

      val notKeepAliveRequest = FCGIBeginRequest(21, FCGIRoles.FCGI_FILTER, keepAlive = false)

      Hex.encodeHexString(notKeepAliveRequest.encode.toArray) shouldEqual "01010015000800000003000000000000"
    }
  }
}
