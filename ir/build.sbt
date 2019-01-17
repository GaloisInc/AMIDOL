import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "amidol",
    libraryDependencies ++= Seq(
      scalaTest % Test,

      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",

      "com.typesafe.akka"      %% "akka-actor"               % "2.5.19",
      "com.typesafe.akka"      %% "akka-stream"              % "2.5.19",
      "com.typesafe.akka"      %% "akka-http"                % "10.1.7",
      "com.typesafe.akka"      %% "akka-http-spray-json"     % "10.1.7"
    )
  )

