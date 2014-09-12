import sbtrelease.ReleasePlugin.ReleaseKeys._
import scoverage.ScoverageSbtPlugin

name := "spray-fastcgi"

Common.settings2_11


libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
  "io.spray" % "spray-can" % "1.2.1",
  "io.spray" % "spray-routing" % "1.2.1",
  "org.specs2" %% "specs2" % "2.4.2" % "test"
)

ScoverageSbtPlugin.instrumentSettings
