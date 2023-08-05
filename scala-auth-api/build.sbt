import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := ""
ThisBuild / organizationName := ""

lazy val root = (project in file("."))
  .settings(
    name := "scala-auth-api",
    libraryDependencies ++= Seq(
      lambdaRuntimeInterfaceClient,
      scalaTest % Test,
      playJson,
      jwt,
      jackson
    ),
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.2",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.15.2",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.2",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.15.2",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.15.2"
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  ).settings(
    assembly / assemblyOutputPath := file("target/function.jar")
  )
