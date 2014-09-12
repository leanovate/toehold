import sbtrelease.ReleasePlugin.ReleaseKeys._
import scoverage.ScoverageSbtPlugin

name := "akka-fastcgi"

Common.settings2_11

libraryDependencies ++= Seq(
                             "com.typesafe.akka" %% "akka-actor" % "2.3.3",
                             "org.scala-stm" %% "scala-stm" % "0.7",
                             "org.specs2" %% "specs2" % "2.4.2" % "test",
                             "com.typesafe.akka" %% "akka-testkit" % "2.3.3" % "test",
                             "commons-codec" % "commons-codec" % "1.7" % "test",
                             "org.hamcrest" % "hamcrest-core" % "1.3" % "test",
                             "org.mockito" % "mockito-core" % "1.9.5" % "test"
                           )

scalacOptions in Test ++= Seq("-Yrangepos")



ScoverageSbtPlugin.instrumentSettings
