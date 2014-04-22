name := "example"

organization := "de.leanovate.toehold"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "play-fastcgi" % "0.1.8"
)     

play.Project.playScalaSettings

de.leanovate.toehold.sbt.PhpFpmPlugin.phpFpmSettings
