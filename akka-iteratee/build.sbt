name := "akka-iteratee"

organization := "de.leanovate.toehold"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.play" %% "play-iteratees" % "2.2.2",
    "org.scalatest" %% "scalatest" % "2.0" % "test"
)
