/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.akka.iteratee.tcp

import akka.util.ByteString

trait RawWriter[A] {
  def write(a: A): ByteString
}

object RawWriter {
  val raw = new RawWriter[ByteString] {
    def write(a: ByteString) = a
  }
}