import sbt._
import sbt.Keys._
import scala.Some
import scala.Some

object Common {
  val settings = Seq(
                      organization := "de.leanovate.toehold",

                      scalaVersion := "2.10.3",

                      publishTo :=
                        Some("Bintray" at "https://api.bintray.com/maven/untoldwind/maven/toehold")
                    )

  val publishSettings = Seq(
                             credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

                           ) ++ aether.Aether.aetherPublishSettings
}