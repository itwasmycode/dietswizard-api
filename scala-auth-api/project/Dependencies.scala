import sbt._

object Dependencies {
  lazy val lambdaRuntimeInterfaceClient = "com.amazonaws" % "aws-lambda-java-runtime-interface-client" % "2.3.1"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.10.0" exclude("com.fasterxml.jackson.core", "jackson-core")
  lazy val jwt = "com.nimbusds" % "nimbus-jose-jwt" % "10.0.1" exclude("com.fasterxml.jackson.core", "jackson-core")

  lazy val xmlBind= "javax.xml.bind" % "jaxb-api" % "2.3.1"

}