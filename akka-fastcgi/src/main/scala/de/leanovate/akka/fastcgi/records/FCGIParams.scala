/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

case class FCGIParams(id: Int, envVars: Seq[(String, String)]) extends FCGIRecord {
  override def typeId = FCGIConstants.FCGI_PARAMS

  override def content = envVars.foldLeft(ByteString.empty) {
    (data, envVar) =>
      data ++ encodeEnvVar(ByteString(envVar._1), ByteString(envVar._2))
  }

  private def encodeEnvVar(name: ByteString, value: ByteString): ByteString = {

    encodeLength(name.length) ++ encodeLength(value.length) ++ name ++ value
  }

  private def encodeLength(len: Int): ByteString = {

    if (len < 0x80) {
      ByteString((len & 0x7f).toByte)
    } else {
      ByteString(
                  (0x80 | (len >> 24)).toByte,
                  ((len >> 16) & 0xff).toByte,
                  ((len >> 8) & 0xff).toByte,
                  (len & 0xff).toByte
                )
    }
  }
}
