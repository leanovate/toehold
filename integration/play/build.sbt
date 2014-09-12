import sbt.Keys._

name := "intergration-play"

organization := "de.leanovate.toehold"

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "play-fastcgi" % "0.2.1-SNAPSHOT"
)

play.Project.playScalaSettings

de.leanovate.toehold.sbt.PhpFpmPlugin.phpFpmSettings
