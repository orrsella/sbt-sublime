sbtPlugin := true

name := "sbt-sublime"

organization := "com.orrsella"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.1"

scalaVersion in Global := "2.10.5"

scalacOptions ++= Seq("-feature")

releaseSettings

// publishing
crossScalaVersions <<= sbtVersion { ver =>
  ver match {
    case v if v.startsWith("0.12.") => Seq("2.9.3", "2.10.5")
    case v if v.startsWith("0.13.") => Seq("2.10.5")
    case _ => sys.error(s"Unknown sbt version [$ver]")
  }
}

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>https://github.com/orrsella/sbt-sublime</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:orrsella/sbt-sublime.git</url>
    <connection>scm:git:git@github.com:orrsella/sbt-sublime.git</connection>
  </scm>
  <developers>
    <developer>
      <id>orrsella</id>
      <name>Orr Sella</name>
      <url>http://orrsella.com</url>
    </developer>
  </developers>
)

// release
publishArtifactsAction := PgpKeys.publishSigned.value
