name := "moxie"

Common.settings

libraryDependencies ++= Seq(
                             "de.leanovate.toehold" %% "akka-fastcgi" % version.value,
                             "com.typesafe.play" %% "play-json" % "2.2.2",
                             "org.specs2" %% "specs2" % "2.1.1" % "test",
                             "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
                             "org.hamcrest" % "hamcrest-core" % "1.3" % "test",
                             "org.mockito" % "mockito-core" % "1.9.5" % "test"
                           )

ScoverageSbtPlugin.instrumentSettings
