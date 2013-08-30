sbtPlugin := true

name := "sbt-sublime"

organization := "com.orrsella"

version := "1.0.8-SNAPSHOT"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.4"

scalaVersion in Global := "2.10.2"

scalacOptions ++= Seq("-feature")

// publishing
crossScalaVersions := Seq("2.10.2")

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
