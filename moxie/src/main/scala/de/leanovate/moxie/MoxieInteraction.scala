/*    _             _           _     _                            *\
**   | |_ ___   ___| |__   ___ | | __| |   License: MIT  (2014)    **
**   | __/ _ \ / _ \ '_ \ / _ \| |/ _` |                           **
**   | || (_) |  __/ | | | (_) | | (_| |                           **
\*    \__\___/ \___|_| |_|\___/|_|\__,_|                           */

package de.leanovate.moxie

import scala.collection.mutable
import de.leanovate.moxie.commands.MoxieCommand

class MoxieInteraction {
  val objectRefs = mutable.Map.empty[Long, MoxieObject]

  def processCommand(command: MoxieCommand) = ???

  def close() = ???
}
