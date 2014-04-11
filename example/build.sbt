name := "example"

organization := "de.leanovate.toehold"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "play-fastcgi" % version.value changing()
)     

play.Project.playScalaSettings

filterSettings

PhpFpmCommands.settings