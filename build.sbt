name               := "Processor"

version            := "0.4.0"

organization       := "de.sciss"

scalaVersion       := "2.11.5"

crossScalaVersions := Seq("2.11.5", "2.10.4")

description        := "A simple mechanism for running asynchronous processes"

homepage           := Some(url("https://github.com/Sciss/" + name.value))

licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

initialCommands in console := """import de.sciss.processor._"""

libraryDependencies ++= Seq(
  "de.sciss"      %% "model"     % "0.3.2",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test"
)

fork := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8")

// ---- console ----

initialCommands in console :=
  """import de.sciss.processor._
    |import scala.concurrent.{Future, blocking, ExecutionContext}
    |""".stripMargin

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
}

// ---- ls.implicit.ly ----

seq(lsSettings :_*)

(LsKeys.tags   in LsKeys.lsync) := Seq("processor", "asynchronous", "worker")

(LsKeys.ghUser in LsKeys.lsync) := Some("Sciss")

(LsKeys.ghRepo in LsKeys.lsync) := Some(name.value)

