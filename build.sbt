
name := "json-home-client"

description := "Scala client to consume JSON home documents"

organization := "de.kaufhof"

version := "1.0.0"

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/Galeria-Kaufhof/jsonhomeclient"))

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.10.5", "2.11.6")

scalacOptions ++= Seq("-language:reflectiveCalls", "-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.typesafe.play" %% "play-test" % "2.3.0" % "test",
  "com.typesafe.play" %% "play-ws" % "2.3.0",
  "com.damnhandy" % "handy-uri-templates" % "2.0.1"
)

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

net.virtualvoid.sbt.graph.Plugin.graphSettings

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
