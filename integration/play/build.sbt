import sbt.Keys._

lazy val root = project.in(file(".")).configs(IntegrationTest)

name := "intergration-play"

organization := "de.leanovate.toehold"

scalaVersion := "2.10.4"

version := "0.1.9-SNAPSHOT"

scalacOptions += "-target:jvm-1.7"

resolvers += "Excilys Repository" at "http://repository.excilys.com/content/groups/public"

libraryDependencies ++= Seq(
                             "de.leanovate.toehold" %% "play-fastcgi" % version.value,
                             "io.gatling" % "gatling-core" % "2.0.0-M3a" % "it, test",
                             "io.gatling" % "gatling-http" % "2.0.0-M3a" % "it, test"
                           )

play.Project.playScalaSettings

de.leanovate.toehold.sbt.PhpFpmPlugin.phpFpmSettings

Defaults.itSettings

sourceDirectory in IntegrationTest := baseDirectory.value / "it"
