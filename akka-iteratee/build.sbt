name := "akka-iteratee"

Common.settings

Common.publishSettings

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.play" %% "play-iteratees" % "2.2.2",
    "org.scalatest" %% "scalatest" % "2.0" % "test"
)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("Bintray Release" at "https://api.bintray.com/maven/untoldwind/maven/toehold")

aether.Aether.aetherPublishSettings
