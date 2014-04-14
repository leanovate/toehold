name := "akka-fastcgi"

Common.settings

Common.publishSettings

libraryDependencies ++= Seq(
                             "com.typesafe.akka" %% "akka-actor" % "2.2.3",
                             "org.scala-stm" %% "scala-stm" % "0.7",
                             "org.scalatest" %% "scalatest" % "2.0" % "test",
                             "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
                             "commons-codec" % "commons-codec" % "1.7" % "test"
                           )
