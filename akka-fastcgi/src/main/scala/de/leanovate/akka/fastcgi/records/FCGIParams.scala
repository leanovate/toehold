package de.leanovate.akka.fastcgi.records

import akka.util.ByteString

case class FCGIParams(id: Int, envVars: Seq[(ByteString, ByteString)]) extends FCGIRecord {
  override def typeId = FCGIConstants.FCGI_PARAMS

  override def content = envVars.foldLeft(ByteString.empty) {
    (data, envVar) =>
      data ++ encodeEnvVar(envVar._1, envVar._2)
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
