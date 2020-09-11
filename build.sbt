lazy val baseName  = "Processor"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "0.4.3"
lazy val mimaVersion    = "0.4.0"

lazy val commonSettings = Seq(
  name               := baseName,
  version            := projectVersion,
  organization       := "de.sciss",
  scalaVersion       := "2.13.3",
  crossScalaVersions := Seq("0.27.0-RC1", "2.13.3", "2.12.12"),
  description        := "A simple mechanism for running asynchronous processes",
  homepage           := Some(url(s"https://git.iem.at/sciss/${name.value}")),
  licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13"),
  unmanagedSourceDirectories in Compile += {
    val sourceDir = (sourceDirectory in Compile).value
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => sourceDir / "scala-2.13+"
      case Some((0, _))            => sourceDir / "scala-2.13+"
      case _                       => sourceDir / "scala-2.13-"
    }
  },
)

lazy val deps = new {
  val main = new {
    val model = "0.3.5"
  }
  val test = new {
    val scalaTest = "3.2.2"
  }
}

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion),
    initialCommands in console := """import de.sciss.processor._""",
    libraryDependencies ++= Seq(
      "de.sciss"      %% "model"     % deps.main.model,
      "org.scalatest" %% "scalatest" % deps.test.scalaTest % Test,
    ),
    fork := true,
    initialCommands in console :=
      """import de.sciss.processor._
        |import scala.concurrent.{Future, blocking, ExecutionContext}
        |""".stripMargin
  )

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = name.value
<scm>
  <url>git@git.iem.at:sciss/{n}.git</url>
  <connection>scm:git:git@git.iem.at:sciss/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
  }
)
