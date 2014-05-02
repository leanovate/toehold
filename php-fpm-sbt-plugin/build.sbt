sbtPlugin := true

name := "php-fpm-sbt-plugin"

Common.settings

addSbtPlugin("com.github.sdb" % "xsbt-filter" % "0.4")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-M1")
