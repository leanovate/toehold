import sbt.Keys._

name := "intergration-play"

organization := "de.leanovate.toehold"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-feature", "-deprecation")

play.Project.playScalaSettings

de.leanovate.toehold.sbt.PhpFpmPlugin.phpFpmSettings
