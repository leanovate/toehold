name := "example"

organization := "de.leanovate.toehold"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "play-fastcgi" % "0.1.6-SNAPSHOT" changing()
)     

play.Project.playScalaSettings

de.leanovate.toehold.sbt.PhpFpmPlugin.phpFpmSettings
