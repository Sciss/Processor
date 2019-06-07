lazy val baseName  = "Processor"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "0.4.2"
lazy val mimaVersion    = "0.4.0"

name               := baseName
version            := projectVersion
organization       := "de.sciss"
scalaVersion       := "2.12.8"
crossScalaVersions := Seq("2.12.8", "2.11.12", "2.13.0")
description        := "A simple mechanism for running asynchronous processes"
homepage           := Some(url(s"https://github.com/Sciss/${name.value}"))
licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion)

initialCommands in console := """import de.sciss.processor._"""

lazy val deps = new {
  val main = new {
    val model = "0.3.4"
  }
  val test = new {
    val scalaTest = "3.0.8-RC5"
  }
}

libraryDependencies ++= Seq(
  "de.sciss" %% "model" % deps.main.model
)

libraryDependencies += {
  val v = deps.test.scalaTest
  if (scalaVersion.value == "2.13.0") {
    "org.scalatest" % "scalatest_2.13.0-RC3" % v % Test
  } else {
    "org.scalatest" %% "scalatest" % v % Test
  }
}


fork := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13")

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
