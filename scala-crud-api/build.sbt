import Dependencies._

ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := ""
ThisBuild / organizationName := ""

lazy val root = (project in file("."))
  .settings(
    name := "scala-crud-api",
    libraryDependencies ++= Seq(
      lambdaRuntimeInterfaceClient,
      scalaTest % Test
    )
  ).settings(
    assembly / assemblyOutputPath := file("target/function.jar")
  )
