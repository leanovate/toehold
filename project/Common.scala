import sbt._
import sbt.Keys._
import scala.Some
import scala.Some

object Common {
  val settings = Seq(
                      organization := "de.leanovate.toehold",

                      scalaVersion := "2.10.3",

                      publishTo := {
                        val nexus = "https://oss.sonatype.org/"
                        if (version.value.trim.endsWith("SNAPSHOT")) {
                          Some("Sonatype Nexus Repository Manager" at nexus + "content/repositories/snapshots")
                        }
                        else {
                          Some("Sonatype Nexus Repository Manager" at nexus + "service/local/staging/deploy/maven2")
                        }
                      }
                      //                        publishTo :=
                      //                        Some("Bintray" at "https://api.bintray.com/maven/untoldwind/maven/toehold")
                    ) ++ com.typesafe.sbt.SbtPgp.settings

  val publishSettings = Seq(
                             credentials += Credentials(Path.userHome / ".ivy2" / ".credentials-sonatype")

                           ) ++ aether.Aether.aetherPublishSettings
}