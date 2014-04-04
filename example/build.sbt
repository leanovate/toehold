name := "example"

organization := "de.leanovate.toehold"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "de.leanovate.toehold" % "play-fastcgi" % "0.0.1-SNAPSHOT" changing()
)     

play.Project.playScalaSettings

filterSettings

PhpFpmCommands.settings