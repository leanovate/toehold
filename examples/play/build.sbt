name := "example"

organization := "de.leanovate.toehold"

scalaVersion := "2.11.6"

lazy val root = (project in file(".")).enablePlugins(PlayScala)


libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "play-fastcgi" % "0.2.3"
)

// play.Project.playScalaSettings

de.leanovate.toehold.sbt.PhpFpmPlugin.phpFpmSettings
