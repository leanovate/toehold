/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie.commands

import play.api.libs.json._
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsSuccess

sealed trait MoxieValue

case class MoxieBoolean(value: Boolean) extends MoxieValue

case class MoxieInt(value: Long) extends MoxieValue

case class MoxieFloat(value: Double) extends MoxieValue

case class MoxieString(value: String) extends MoxieValue

case class MoxieArray(keyValues: Seq[(String, MoxieValue)])

case class MoxieObjectRef(objectId: String) extends MoxieValue

object MoxieValue {
  implicit val moxieReads = new Reads[MoxieValue] {
    override def reads(json: JsValue): JsResult[MoxieValue] = json match {
      case JsBoolean(value) => JsSuccess(MoxieBoolean(value))
      case JsNumber(value) if value.isValidLong => JsSuccess(MoxieInt(value.toLongExact))
      case JsNumber(value) if value.isValidDouble => JsSuccess(MoxieFloat(value.toDouble))
      case JsString(value) => JsSuccess(MoxieString(value))
      case _ => JsError(s"Unknown js type $json")
    }
  }
}