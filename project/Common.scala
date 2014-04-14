import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.SonatypeKeys._

object Common {
  val settings = Seq(
                      organization := "de.leanovate.toehold",

                      scalaVersion := "2.10.3",

                      profileName := "de.leanovate",

                      pomExtra := {
                        <url>https://github.com/leanovate/toehold</url>
                          <licenses>
                            <license>
                              <name>MIT</name>
                              <url>http://opensource.org/licenses/MIT</url>
                            </license>
                          </licenses>
                          <scm>
                            <connection>scm:git:github.com/leanovate/toehold</connection>
                            <developerConnection>scm:git:git@github.com:/leanovate/toehold</developerConnection>
                            <url>github.com/leanovate/toehold</url>
                          </scm>
                          <developers>
                            <developer>
                              <id>untoldwind</id>
                              <name>Bodo Junglas</name>
                              <url>http://untoldwind.github.io/</url>
                            </developer>
                          </developers>
                      }
                    ) ++ xerial.sbt.Sonatype.sonatypeSettings ++ com.typesafe.sbt.SbtPgp.settings

  val publishSettings = Seq(
                             credentials += Credentials(Path.userHome / ".ivy2" / ".credentials-sonatype")

                           )
}