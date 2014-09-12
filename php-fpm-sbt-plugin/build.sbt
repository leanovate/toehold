import sbtrelease.ReleasePlugin.ReleaseKeys._

sbtPlugin := true

name := "php-fpm-sbt-plugin"

Common.settings2_10


addSbtPlugin("com.github.sdb" % "xsbt-filter" % "0.4")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.4")

