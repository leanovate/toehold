name := "parent"

organization := "de.leanovate.toehold"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.3"

lazy val root = project.in(file(".")).aggregate(akkaIteratee, akkaFastCgi, playFastCgi)

lazy val akkaIteratee = project.in(file("akka-iteratee"))

lazy val akkaFastCgi = project.in(file("akka-fastcgi")).dependsOn(akkaIteratee)

lazy val playFastCgi = project.in(file("play-fastcgi")).dependsOn(akkaFastCgi)
