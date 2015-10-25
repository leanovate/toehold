
name := "play-fastcgi"

Common.settings2_11


libraryDependencies ++= Seq(
    "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
    "com.typesafe.play" %% "play" % "2.4.3",
    "org.specs2" %% "specs2" % "2.4.2" % "test",
    "com.typesafe.play" %% "play-test" % "2.4.3" % "test"
)
