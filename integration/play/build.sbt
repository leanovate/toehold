import sbt.Keys._

name := "intergration-play"

organization := "de.leanovate.toehold"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "play-fastcgi" % "0.1.9-SNAPSHOT"
)

play.Project.playScalaSettings

de.leanovate.toehold.sbt.PhpFpmPlugin.phpFpmSettings
