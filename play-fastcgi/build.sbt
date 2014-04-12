name := "play-fastcgi"

Common.settings

Common.publishSettings

libraryDependencies ++= Seq(
    "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
    "com.typesafe.play" %% "play" % "2.2.2",
    "org.scalatest" %% "scalatest" % "2.0" % "test"
)
