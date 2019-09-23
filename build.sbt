import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "amidol",
    scalacOptions ++= Seq(
      "-unchecked",
      "-feature",
      "-deprecation",

      "-language:implicitConversions"
    ),
    libraryDependencies ++= Seq(
      scalaTest % Test,
      // Akka web stack
      "com.typesafe.akka"      %% "akka-actor"               % "2.5.19",
      "com.typesafe.akka"      %% "akka-stream"              % "2.5.19",
      "com.typesafe.akka"      %% "akka-http"                % "10.1.7",
      "com.typesafe.akka"      %% "akka-http-spray-json"     % "10.1.7",

      // Configuration
      "com.typesafe"           % "config"                    % "1.3.1",

      // Parsing
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",

      // SQLite database access
      "org.xerial"             % "sqlite-jdbc"               % "3.28.0",

      // WebJars (javascript dependencies masquerading as JARs)
      "org.webjars"            % "webjars-locator"           % "0.36",
      "org.webjars"            % "jquery"                    % "3.4.1",
      "org.webjars"            % "visjs"                     % "4.21.0",
      "org.webjars.bower"      % "underscore"                % "1.9.1",
      "org.webjars.npm"        % "chartist"                  % "0.11.0",
      "org.webjars.npm"        % "chartist-plugin-axistitle" % "0.0.4",
      "org.webjars.bower"      % "plotly.js"                 % "1.48.3"
    ),
  )
  .enablePlugins(SbtWeb)

