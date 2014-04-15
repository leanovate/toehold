// Comment to get more information during initialization
logLevel := Level.Info

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.2")

addSbtPlugin("com.github.sdb" % "xsbt-filter" % "0.4")

addSbtPlugin( "de.leanovate.toehold" %% "php-fpm-sbt-plugin" % "0.1.5-SNAPSHOT")
