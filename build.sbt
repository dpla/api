lazy val akkaHttpVersion = "10.2.7"
lazy val akkaVersion     = "2.6.17"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "dpla",
      scalaVersion    := "2.13.4"
    )),

    name := "ebook-api",
    assembly / mainClass := Some("dpla.ebookapi.RunApp"),
    assembly / assemblyJarName := "dpla-ebooks-api.jar",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-http"               % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-http-spray-json"    % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-actor-typed"        % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream"             % akkaVersion,
      "ch.qos.logback"     % "logback-classic"          % "1.2.3",
      "com.typesafe.slick" %% "slick"                   % "3.3.3",
      "com.typesafe.slick" %% "slick-hikaricp"          % "3.3.3",
      "org.postgresql"     %  "postgresql"              % "9.4-1206-jdbc42",
      "commons-validator"  %  "commons-validator"       % "1.7",
      "commons-codec"      %  "commons-codec"           % "1.15",
      "com.amazonaws"      %  "aws-java-sdk-ses"        % "1.12.163",


"com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.1.4"         % Test
    )
  )

ThisBuild / assemblyMergeStrategy := {
  case "reference.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case x => MergeStrategy.first
}

