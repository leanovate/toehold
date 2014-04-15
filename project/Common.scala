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

           scalaVersion := "2.10.3",

           profileName := "de.leanovate",

           resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",

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
}