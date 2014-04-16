name := "play-fastcgi"

Common.settings

libraryDependencies ++= Seq(
    "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
    "com.typesafe.play" %% "play" % "2.2.2",
    "org.specs2" %% "specs2" % "2.1.1" % "test",
    "com.typesafe.play" %% "play-test" % "2.2.2" % "test"
)

ScoverageSbtPlugin.instrumentSettings
