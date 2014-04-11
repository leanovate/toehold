import sbt._
import sbt.Keys._
import scala.Some

object Common {
  val settings = Seq(
                      organization := "de.leanovate.toehold",

                      version := "0.0.2",

                      scalaVersion := "2.10.3"

                    )

  val publishSettings = Seq(
                             credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

                             publishTo :=
                               Some("Bintray" at "https://api.bintray.com/maven/untoldwind/maven/toehold")
                           ) ++ aether.Aether.aetherPublishSettings
}