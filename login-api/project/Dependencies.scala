import sbt._

object Dependencies {
  lazy val lambdaRuntimeInterfaceClient = "com.amazonaws" % "aws-lambda-java-runtime-interface-client" % "2.3.1"
  lazy val secretManager = "com.amazonaws" % "aws-java-sdk-secretsmanager" % "1.11.1034"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.9.4"
  lazy val jwt = "com.nimbusds" % "nimbus-jose-jwt" % "9.12"
  lazy val xmlBind= "javax.xml.bind" % "jaxb-api" % "2.3.1"
  lazy val jackson= "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.2"
  lazy val slick = "com.typesafe.slick" %% "slick" % "3.3.3"
  lazy val postgresql = "org.postgresql" % "postgresql" % "42.2.8"
  lazy val bcrypt= "at.favre.lib" % "bcrypt" % "0.10.2"
  lazy val logging = "org.slf4j" % "slf4j-api" % "1.7.30"
  lazy val loggingforj = "org.slf4j" % "slf4j-log4j12" % "1.7.30"
  lazy val awsjavasdk =  "com.amazonaws" % "aws-java-sdk" % "1.9.9" // Replace with the appropriate version
}