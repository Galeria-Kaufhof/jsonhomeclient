import ReleaseTransformations._

name := "json-home-client"

description := "Scala client to consume JSON home documents"

organization := "de.kaufhof"

version := "3.0.1"

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/Galeria-Kaufhof/jsonhomeclient"))

scalaVersion := "2.11.9"

scalacOptions ++= Seq("-language:reflectiveCalls", "-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.mockito" % "mockito-core" % "2.7.20" % "test",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.15.0",
  "com.typesafe.akka" %% "akka-http" % "10.0.5",
  "com.typesafe.play" %% "play-json" % "2.6.0-M6",
  "ch.qos.logback" % "logback-core" % "1.2.2",
  "ch.qos.logback" % "logback-classic" % "1.2.2",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "com.damnhandy" % "handy-uri-templates" % "2.1.6"
)

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Publish settings
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:Galeria-Kaufhof/jsonhomeclient.git</url>
    <connection>scm:git:git@github.com:Galeria-Kaufhof/jsonhomeclient.git</connection>
  </scm>
    <developers>
      <developer>
        <id>martin.grotzke</id>
        <name>Martin Grotzke</name>
        <url>https://github.com/magro</url>
      </developer>
      <developer>
        <id>manuel.kiessling</id>
        <name>Manuel Kiessling</name>
        <url>https://github.com/manuelkiessling</url>
      </developer>
      <developer>
        <id>fabian.koehler</id>
        <name>Fabian Koehler</name>
        <url>https://github.com/fkoehler</url>
      </developer>
      <developer>
        <id>markus.klink</id>
        <name>Markus Klink</name>
        <url>https://github.com/justjoheinz</url>
      </developer>
    </developers>)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)
