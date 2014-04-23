package de.leanovate.play.fastcgi

import java.io.File
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration

case class FastCGISettings(
  documentRoot: File,
  requestTimeout: Timeout,
  suspendTimeout: FiniteDuration,
  host: String,
  port: Int,
  fileWhiteList: Set[String]
  )