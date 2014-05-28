
organization := "gkh.jsonhomeclient"

name := """jsonhomeclient"""

version := Option(System.getProperty("version")) getOrElse "0.1.0-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-language:reflectiveCalls", "-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.0" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.typesafe.play" %% "play-test" % "2.2.3" % "test",
  "com.typesafe.play" %% "play" % "2.2.3"
)

publishMavenStyle := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

publishTo := Some("Peng Releases" at "http://nexus.gkh-setu.de/content/repositories/releases/")

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.gkh-setu.de", "deployment", "deployment")
