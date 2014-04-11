name := "example"

Common.settings

libraryDependencies ++= Seq(
  "de.leanovate.toehold" %% "play-fastcgi" % version.value
)     

play.Project.playScalaSettings

filterSettings

PhpFpmCommands.settings
