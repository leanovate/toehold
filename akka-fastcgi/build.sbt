name := "akka-fastcgi"

Common.settings

Common.publishSettings

libraryDependencies ++= Seq(
    "de.leanovate.toehold" %% "akka-iteratee" % version.value,
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.play" %% "play-iteratees" % "2.2.2",
    "org.scalatest" %% "scalatest" % "2.0" % "test",
    "commons-codec" % "commons-codec" % "1.7" % "test"
)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("Bintray Release" at "https://api.bintray.com/maven/untoldwind/maven/toehold")

aether.Aether.aetherPublishSettings
