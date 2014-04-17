package de.leanovate.play.fastcgi

import java.io.File
import akka.util.Timeout

case class FastCGISettings(
  documentRoot: File,
  timeout: Timeout,
  host: String,
  port: Int,
  fileWhiteList: Set[String]
  )
