name := "recaptcha"

organization := "com.nappin.play"

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  ws,
  "org.mockito" % "mockito-core" % "1.+" % "test"
)

