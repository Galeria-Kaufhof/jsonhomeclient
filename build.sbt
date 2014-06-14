
organization := "gkh.jsonhomeclient"

name := """jsonhomeclient"""

version := Option(System.getProperty("version")) getOrElse "0.1.0-SNAPSHOT"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.10.4", "2.11.1")

scalacOptions ++= Seq("-language:reflectiveCalls", "-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.typesafe.play" %% "play-test" % "2.3.0" % "test",
  "com.typesafe.play" %% "play-ws" % "2.3.0",
  "com.damnhandy" % "handy-uri-templates" % "2.0.1"
)

publishMavenStyle := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

publishTo := Some("Peng Releases" at "http://nexus.gkh-setu.de/content/repositories/releases/")

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.gkh-setu.de", "deployment", "deployment")
