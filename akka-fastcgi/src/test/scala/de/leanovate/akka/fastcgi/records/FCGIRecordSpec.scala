/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.records

import akka.util.ByteString
import org.apache.commons.codec.binary.Hex
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers

class FCGIRecordSpec extends Specification with ShouldMatchers {

  "FCGIRequest.decode" should {
    "decode stdout record" in {
      val responseData = ByteString(Hex
        .decodeHex(
          " 01 06 00 01 00 3f 01 00 58 2d 50 6f 77 65 72 65 64 2d 42 79 3a 20 50 48 50 2f 35 2e 33 2e 33 0d 0a 43 6f 6e 74 65 6e 74 2d 74 79 70 65 3a 20 74 65 78 74 2f 68 74 6d 6c 0d 0a 0d 0a 48 65 6c 6c 6f 20 57 6f 72 6c 64 00 01 03 00 01 00 08 00 00 00 00 00 00 00 69 61 62"
            .replace(" ", "").toCharArray))

      FCGIRecord.decode(responseData) match {
        case (Some(FCGIStdOut(id, content)), remain) =>
          id shouldEqual 1
          content.utf8String.replace("\r\n", "\n") shouldEqual """X-Powered-By: PHP/5.3.3
                                                                 |Content-type: text/html
                                                                 |
                                                                 |Hello World""".stripMargin
          Hex.encodeHexString(remain.toArray) shouldEqual "01030001000800000000000000696162"
        case noMatch =>
          failure("$noMatch is unexpected")
      }
    }

    "decode end request record" in {
      val responseData = ByteString(Hex.decodeHex("01030001000800000000000000696162".toCharArray))

      FCGIRecord.decode(responseData) match {
        case (Some(FCGIEndRequest(id)), remain) =>
          id should be_==(1)
          remain.isEmpty should beTrue
        case noMatch =>
          failure("$noMatch is unexpected")
      }
    }
  }
}