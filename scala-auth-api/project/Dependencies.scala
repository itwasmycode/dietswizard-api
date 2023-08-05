import sbt._

object Dependencies {
  lazy val lambdaRuntimeInterfaceClient = "com.amazonaws" % "aws-lambda-java-runtime-interface-client" % "2.3.1"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.9.2"
}