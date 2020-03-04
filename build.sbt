import com.typesafe.sbt.web.SbtWeb
import sbt.Keys._
import sbt._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    resourceGenerators in Compile += WebpackNpm.webpack,
    WebpackNpm.projectName := "web-react",
    WebpackNpm.npmProjectDir := (resourceDirectory in Compile).value / "web-react",
    WebpackNpm.outputDir := (resourceManaged in Compile).value / "web",
    WebpackNpm.webpack := WebpackNpm.npmWebpackTask.value,
  )
  .settings(
    name := "amidol",
    scalacOptions ++= Seq(
      "-unchecked",
      "-feature",
      "-deprecation",

      "-language:implicitConversions"
    ),

    libraryDependencies ++= Seq(
      // Akka web stack
      "com.typesafe.akka"      %% "akka-actor"               % "2.6.3",
      "com.typesafe.akka"      %% "akka-stream"              % "2.6.3",
      "com.typesafe.akka"      %% "akka-http"                % "10.1.11",
      "com.typesafe.akka"      %% "akka-http-spray-json"     % "10.1.11",

      // Configuration
      "com.typesafe"           % "config"                    % "1.3.1",

      // Parsing
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",

      // SQLite database access
      "org.xerial"             % "sqlite-jdbc"               % "3.28.0",

      // WebJars (javascript dependencies masquerading as JARs)
      "org.webjars"            % "webjars-locator"           % "0.36",
      "org.webjars"            % "jquery"                    % "3.4.1",
      "org.webjars"            % "visjs"                     % "4.21.0",
      "org.webjars"            % "bootstrap"                 % "4.3.1",
      "org.webjars.bower"      % "underscore"                % "1.9.1",
      "org.webjars.bower"      % "plotly.js"                 % "1.50.1",
      "org.webjars"            % "ionicons"                  % "2.0.1",


      // Testing
      "org.scalatest"          %% "scalatest"                % "3.0.8" % Test,

      // Documentation of endpoints
      "org.julienrf"           %% "endpoints-algebra-json-schema" % "0.14.0",
      "org.julienrf"           %% "endpoints-json-schema-generic" % "0.14.0",
      "org.julienrf"           %% "endpoints-akka-http-server"    % "0.14.0",
    ),
  )
  .enablePlugins(SbtWeb)
