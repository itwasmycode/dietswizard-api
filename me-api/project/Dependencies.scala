import sbt._

object Dependencies {
  lazy val lambdaRuntimeInterfaceClient = "com.amazonaws" % "aws-lambda-java-runtime-interface-client" % "2.3.1"
  lazy val secretManager = "com.amazonaws" % "aws-java-sdk-secretsmanager" % "1.12.118" // updated version
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10" // updated version
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.9.4"
  lazy val jwt = "com.nimbusds" % "nimbus-jose-jwt" % "9.12.1" // updated version
  lazy val xmlBind= "javax.xml.bind" % "jaxb-api" % "2.3.1"
  lazy val jackson= "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.2"
  lazy val slick = "com.typesafe.slick" %% "slick" % "3.3.3"
  lazy val postgresql = "org.postgresql" % "postgresql" % "42.2.23" // updated version
  lazy val bcrypt= "at.favre.lib" % "bcrypt" % "0.10.2"
  lazy val logging = "org.slf4j" % "slf4j-api" % "1.7.32" // updated version
  lazy val loggingforj = "org.slf4j" % "slf4j-log4j12" % "1.7.32" // updated version
  lazy val awsjavasdk =  "com.amazonaws" % "aws-java-sdk" % "1.12.536" // updated version
  lazy val jacksonScala= "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2"
  lazy val lambdaJavaCore= "com.amazonaws" % "aws-lambda-java-core" % "1.2.3"
  lazy val lambdaJavaEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.11.2"
  lazy val circeCore = "io.circe" %% "circe-core" % "0.14.1"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.14.1"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.14.1"
  lazy val jsonforJacks = "org.json4s" %% "json4s-native" % "3.6.11"
}
