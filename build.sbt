name := "parent"

Common.settings

lazy val root = project.in(file(".")).settings(publish := {}).aggregate(akkaFastCgi, playFastCgi, example)

lazy val akkaFastCgi = project.in(file("akka-fastcgi"))

lazy val playFastCgi = project.in(file("play-fastcgi")).dependsOn(akkaFastCgi)

lazy val example = project.in(file("example")).settings(publish := {}).dependsOn(playFastCgi)

sbtrelease.ReleasePlugin.releaseSettings