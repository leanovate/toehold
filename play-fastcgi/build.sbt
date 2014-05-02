name := "play-fastcgi"

Common.settings

libraryDependencies ++= Seq(
    "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
    "com.typesafe.play" %% "play" % "2.3-M1",
    "org.specs2" %% "specs2" % "2.3.10" % "test",
    "com.typesafe.play" %% "play-test" % "2.3-M1" % "test"
)

ScoverageSbtPlugin.instrumentSettings
