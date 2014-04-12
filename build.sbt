name := "parent"

Common.settings

lazy val root = project.in(file(".")).settings(publish := {}).aggregate(akkaIteratee, akkaFastCgi, playFastCgi, example)

lazy val akkaIteratee = project.in(file("akka-iteratee"))

lazy val akkaFastCgi = project.in(file("akka-fastcgi")).dependsOn(akkaIteratee)

lazy val playFastCgi = project.in(file("play-fastcgi")).dependsOn(akkaFastCgi)

lazy val example = project.in(file("example")).settings(publish := {}).dependsOn(playFastCgi)

sbtrelease.ReleasePlugin.releaseSettings