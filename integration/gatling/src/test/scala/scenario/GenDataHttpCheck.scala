/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package scenario

import io.gatling.http.check.{HttpCheckOrder, HttpCheck}
import io.gatling.core.check.Check
import io.gatling.http.response.Response
import io.gatling.core.session.Session
import io.gatling.core.validation.{Validation, Failure, Success}
import java.io.{InputStreamReader, BufferedReader}
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest

class GenDataHttpCheck extends Check[Response] {
  override def check(response: Response, session: Session)(implicit cache: Cache): Validation[Session] = {
    val reader = new BufferedReader(new InputStreamReader(response.getResponseBodyAsStream, "UTF-8"))

    val firstLine = reader.readLine()

    if (firstLine == null)
      Failure("No lines")
    else {
      val md5 = hex2bin(firstLine)
      Stream.continually(reader.readLine()).takeWhile(_ ne null).foreach {
        line =>
          if (calcMD5(line) == md5)
            return Failure("Invalid MD5")
      }
      Success(session)
    }
  }

  def hex2bin(str: String) = Hex.decodeHex(str.trim.toCharArray)

  def calcMD5(str: String) = {
    val md = MessageDigest.getInstance("MD5")

    md.digest(str.getBytes("UTF-8"))
  }
}

object GenDataHttpCheck {
  def check = HttpCheck(new GenDataHttpCheck, HttpCheckOrder.Body)
}