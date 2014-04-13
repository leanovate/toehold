name := "akka-fastcgi"

Common.settings

Common.publishSettings

libraryDependencies ++= Seq(
                             "com.typesafe.akka" %% "akka-actor" % "2.2.3",
                             "com.typesafe.play" %% "play-iteratees" % "2.2.2",
                             "org.scalatest" %% "scalatest" % "2.0" % "test",
                             "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
                             "commons-codec" % "commons-codec" % "1.7" % "test"
                           )
