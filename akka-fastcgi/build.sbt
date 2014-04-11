name := "akka-fastcgi"

organization := "de.leanovate.toehold"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
    "de.leanovate.toehold" %% "akka-iteratee" % version.value,
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.play" %% "play-iteratees" % "2.2.2",
    "org.scalatest" %% "scalatest" % "2.0" % "test",
    "commons-codec" % "commons-codec" % "1.7" % "test"
)
