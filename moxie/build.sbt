import scoverage.ScoverageSbtPlugin

name := "moxie"

Common.settings2_11

libraryDependencies ++= Seq(
                             "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
                             "com.typesafe.play" %% "play-json" % "2.4.3",
                             "org.specs2" %% "specs2" % "2.4.2" % "test",
                             "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
                             "org.hamcrest" % "hamcrest-core" % "1.3" % "test",
                             "org.mockito" % "mockito-core" % "1.9.5" % "test"
                           )

ScoverageSbtPlugin.instrumentSettings
