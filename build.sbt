lazy val akkaHttpVersion = "10.4.0"
lazy val akkaVersion     = "2.7.0"

lazy val openTelemetrySpecific = {
  val version = "1.45.0"
  Seq(
    "io.opentelemetry" % "opentelemetry-bom" % version pomOnly(),
    "io.opentelemetry" % "opentelemetry-api" % version,
    "io.opentelemetry" % "opentelemetry-sdk" % version,
    "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % version,
    "io.opentelemetry" % "opentelemetry-exporter-otlp" % version
  )
}

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    inThisBuild(List(
      organization    := "dpla",
      scalaVersion    := "2.13.4"
    )),
    Defaults.itSettings,

    name := "api",
    assembly / mainClass := Some("dpla.api.RunApp"),
    assembly / assemblyJarName := "dpla-api.jar",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-http"               % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-http-spray-json"    % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-actor-typed"        % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-s3"  % "5.0.0",
      "ch.qos.logback"     % "logback-classic"          % "1.2.11",
      "com.typesafe.slick" %% "slick"                   % "3.3.3",
      "com.typesafe.slick" %% "slick-hikaricp"          % "3.3.3",
      "org.postgresql"     %  "postgresql"              % "42.3.3",
      "commons-validator"  %  "commons-validator"       % "1.7",
      "commons-codec"      %  "commons-codec"           % "1.15",
      "com.amazonaws"      %  "aws-java-sdk-ses"        % "1.12.173",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.11"        % Test,

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % IntegrationTest,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % IntegrationTest,
      "org.scalatest"     %% "scalatest"                % "3.2.11"        % IntegrationTest
    ) ++ openTelemetrySpecific,

  )

ThisBuild / assemblyMergeStrategy := {
  case "reference.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case x => MergeStrategy.first
}
