
name := "integration-gatling"

organization := "de.leanovate.toehold"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-feature", "-deprecation")

resolvers += "Excilys Repository" at "http://repository.excilys.com/content/groups/public"

libraryDependencies ++= Seq(
                             "io.gatling" % "gatling-core" % "2.0.0-M3a" % "test",
                             "io.gatling" % "gatling-http" % "2.0.0-M3a" % "test"
                           )