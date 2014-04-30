package de.leanovate.spray.fastcgi

import java.io.File
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration

case class FastCGISettings(
                            documentRoot: File,
                            requestTimeout: FiniteDuration,
                            suspendTimeout: FiniteDuration,
                            maxConnections: Int,
                            host: String,
                            port: Int,
                            fileWhiteList: Set[String]
                            )