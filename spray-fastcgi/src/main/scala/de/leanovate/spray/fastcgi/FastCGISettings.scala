/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

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