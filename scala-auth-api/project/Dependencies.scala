import sbt._

object Dependencies {
  lazy val lambdaRuntimeInterfaceClient = "com.amazonaws" % "aws-lambda-java-runtime-interface-client" % "2.3.1"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.9.4"
  lazy val jwt = "com.nimbusds" % "nimbus-jose-jwt" % "9.12"
  lazy val xmlBind= "javax.xml.bind" % "jaxb-api" % "2.3.1"
  lazy val jackson= "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.2"
}