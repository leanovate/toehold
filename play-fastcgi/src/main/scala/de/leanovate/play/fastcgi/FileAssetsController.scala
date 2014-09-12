/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.play.fastcgi

import play.api.mvc._
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone
import java.io.File
import scala.collection.JavaConversions._
import scala.util.control.NonFatal
import play.api.libs.Codecs
import play.api.Play.current
import play.api.Play.configuration

trait FileAssetsController extends Controller {
  private val timeZoneCode = "GMT"

  private val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'").withLocale(java.util.Locale.ENGLISH)
      .withZone(DateTimeZone.forID(timeZoneCode))

  private val dfp: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH)
      .withZone(DateTimeZone.forID(timeZoneCode))

  private val parsableTimezoneCode = " " + timeZoneCode

  def serveFile(path: String, documentRoot: Option[String] = None) = Action {
    request =>
      val idx = path.lastIndexOf('.')
      if (idx < 0) {
        Forbidden
      } else {
        val extension = path.substring(idx + 1).toLowerCase

        if (!settings.fileWhiteList.contains(extension)) {
          Forbidden
        } else {
          val file = new File(documentRoot.map(new File(_)).getOrElse(settings.documentRoot), path)

          if (!file.exists() || !file.isFile) {
            NotFound
          } else {
            maybeNotModified(request, file).getOrElse {
              Ok.sendFile(file).withHeaders(ETAG -> calcEtag(file),
                                             DATE -> df.print(file.lastModified()))
            }
          }
        }
      }
  }

  protected def maybeNotModified(request: RequestHeader, file: File): Option[Result] = {

    request.headers.get(IF_NONE_MATCH) match {
      case Some(etags) =>
        val etag = calcEtag(file)
        etags.split(",").find(_.trim == etag).map(_ => NotModified)
      case None =>
        val lastModified = file.lastModified()
        request.headers.get(IF_MODIFIED_SINCE).flatMap(parseDate).filter(_ >= lastModified).map(_ => NotModified)
    }
  }

  protected def parseDate(date: String): Option[Long] = try {
    //jodatime does not parse timezones, so we handle that manually
    val d = dfp.parseDateTime(date.replace(parsableTimezoneCode, "")).getMillis
    Some(d)
  } catch {
    case NonFatal(_) => None
  }

  protected def calcEtag(file: File): String = {

    Codecs.sha1(file.getName + file.lastModified() + file.length())
  }

  protected def settings: FastCGISettings = FastCGIPlugin.settings
}

object FileAssetsController extends FileAssetsController {
}