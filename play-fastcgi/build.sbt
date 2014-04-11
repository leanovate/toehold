name := "play-fastcgi"

organization := "de.leanovate.toehold"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
    "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
    "com.typesafe.play" %% "play" % "2.2.2",
    "org.scalatest" %% "scalatest" % "2.0" % "test"
)
