name := "spray-fastcgi"

Common.settings

libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
  "io.spray" % "spray-can" % "1.2.1",
  "org.specs2" %% "specs2" % "2.1.1" % "test"
)

ScoverageSbtPlugin.instrumentSettings
