// Comment to get more information during initialization
logLevel := Level.Info

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Excilys Repository" at "http://repository.excilys.com/content/groups/public"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.2")

addSbtPlugin("com.github.sdb" % "xsbt-filter" % "0.4")

addSbtPlugin( "de.leanovate.toehold" %% "php-fpm-sbt-plugin" % "0.1.9-SNAPSHOT")