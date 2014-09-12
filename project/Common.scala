import com.typesafe.sbt.pgp.PgpKeys._
import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleaseStep
import xerial.sbt.Sonatype.SonatypeKeys._

object Common {
  lazy val publisSignedhArtifactsAction = {
    st: State =>
      val extracted = Project.extract(st)
      val ref = extracted.get(thisProjectRef)
      extracted.runAggregated(publishSigned in Global in ref, st)
  }

  val settings =
    xerial.sbt.Sonatype.sonatypeSettings ++
      com.typesafe.sbt.SbtPgp.settings ++
      sbtrelease.ReleasePlugin.releaseSettings ++
      Seq(
           organization := "de.leanovate.toehold",

           scalacOptions ++= Seq("-feature", "-deprecation"),

           profileName := "de.leanovate",

           resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/", "Typesafe maven repository" at "http://repo.typesafe.com/typesafe/maven-releases/"),

           publishMavenStyle := true,

          libraryDependencies := {
            CrossVersion.partialVersion(scalaBinaryVersion.value) match {
              // if scala 2.11+ is used, add dependency on scala-xml module
              case Some((2, scalaMajor)) if scalaMajor >= 11 =>
                libraryDependencies.value ++ Seq(
                  "org.scala-lang.modules" % "scala-xml" % "1.0.2" cross CrossVersion.binary)
              case _ =>
                // or just libraryDependencies.value if you don't depend on scala-swing
                libraryDependencies.value
            }
          },

          pomExtra := {
             <url>https://github.com/leanovate/toehold</url>
               <licenses>
                 <license>
                   <name>MIT</name>
                   <url>http://opensource.org/licenses/MIT</url>
                 </license>
               </licenses>
               <scm>
                 <connection>scm:git:github.com/leanovate/toehold</connection>
                 <developerConnection>scm:git:git@github.com:/leanovate/toehold</developerConnection>
                 <url>github.com/leanovate/toehold</url>
               </scm>
               <developers>
                 <developer>
                   <id>untoldwind</id>
                   <name>Bodo Junglas</name>
                   <url>http://untoldwind.github.io/</url>
                 </developer>
               </developers>
           },

           releaseProcess := Seq[ReleaseStep](
                                               checkSnapshotDependencies,
                                               inquireVersions,
                                               runClean,
                                               runTest,
                                               setReleaseVersion,
                                               commitReleaseVersion,
                                               tagRelease,
                                               publishArtifacts.copy(action = publisSignedhArtifactsAction),
                                               setNextVersion,
                                               commitNextVersion,
                                               pushChanges
                                             )
         )

  def settings2_11 = settings ++ Seq(
    scalaVersion := propOr("toehold.scalaVersion", "2.11.2")
    // scalaBinaryVersion := "2.11"
    //crossBuild := true,
    //crossScalaVersions := Seq("2.10.4", "2.11.2")
  )

  /**
   * The SBT plugin only supports scala 2.10.
   */
  def settings2_10 = settings ++ Seq(
    scalaVersion := "2.10.4"
    // scalaBinaryVersion := "2.10"
  )

  def propOr(name: String, value: String): String =
    (sys.props get name) orElse
      (sys.env get name) getOrElse
      value

}