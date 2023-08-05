import Dependencies._

ThisBuild / scalaVersion     := "2.13.5"
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
      xmlBind
    )
  ).settings(
    assembly / assemblyOutputPath := file("target/function.jar")
  )
