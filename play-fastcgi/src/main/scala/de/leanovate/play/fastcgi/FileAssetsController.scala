/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import play.api.mvc.{Action, Controller}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone
import java.io.File

object FileAssetsController extends Controller {
  private val whitelist = Set("gif", "png", "js", "css", "jpg")
  private val timeZoneCode = "GMT"

  private val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  def file(documentRoot: String, path: String) = Action {
    val idx = path.lastIndexOf('.')
    if ( idx < 0 )
      Forbidden
    else {
      val extension = path.substring(idx + 1).toLowerCase

      if (!whitelist.contains(extension))
        Forbidden
      else {
        val file = new File(documentRoot + "/" + path)

        if (!file.exists() || !file.isFile)
          NotFound


        Ok.sendFile(file)
      }
    }
  }
}
