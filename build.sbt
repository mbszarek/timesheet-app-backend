import Dependencies._
import sbt._
import Path._

enablePlugins(
  JavaAppPackaging,
)

lazy val dockerSettings: Seq[SettingsDefinition] = Seq(
  maintainer in Docker := "Mateusz Szarek <mszarek@student.agh.edu.pl>",
  packageSummary in Docker := "Containerized Timesheet App",
  packageDescription := "Timesheet App Service",
  mappings in Universal ++= {
    (baseDirectory.value / "config" ** "*")
      .pair(relativeTo(baseDirectory.value / "config"))
      .map { case (file, path) => file -> s"config/$path" }
  },
  dockerExposedPorts in Docker := Seq(8080),
  daemonUser in Docker := "timesheet",
  scriptClasspath in bashScriptDefines ~= (cp => "../config" +: cp),
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(dockerSettings: _*)
  .settings(
    organization := "com.timesheet",
    name := "timesheet_app",
    version := "1.0.0",
    scalaVersion := ProjectScalaVersion,
    scalafmtOnCompile := true,
    mainClass in Compile := Some("com.timesheet.Main"),
    unmanagedClasspath in Runtime += baseDirectory.value / "config",
    libraryDependencies ++= Seq(
        "org.typelevel"         %% "cats-core"              % CatsVersion,
        "org.typelevel"         %% "cats-effect"            % CatsVersion,
        "org.http4s"            %% "http4s-blaze-server"    % Http4sVersion,
        "org.http4s"            %% "http4s-blaze-client"    % Http4sVersion,
        "org.http4s"            %% "http4s-circe"           % Http4sVersion,
        "org.http4s"            %% "http4s-dsl"             % Http4sVersion,
        "io.circe"              %% "circe-generic"          % CirceVersion,
        "org.specs2"            %% "specs2-core"            % Specs2Version % Test,
        "org.scalatest"         %% "scalatest"              % ScalaTestVersion % Test,
        "ch.qos.logback"        % "logback-classic"         % LogbackVersion,
        "io.monix"              %% "monix"                  % MonixVersion,
        "io.github.jmcardon"    %% "tsec-common"            % tsecV,
        "io.github.jmcardon"    %% "tsec-password"          % tsecV,
        "io.github.jmcardon"    %% "tsec-cipher-jca"        % tsecV,
        "io.github.jmcardon"    %% "tsec-cipher-bouncy"     % tsecV,
        "io.github.jmcardon"    %% "tsec-mac"               % tsecV,
        "io.github.jmcardon"    %% "tsec-signatures"        % tsecV,
        "io.github.jmcardon"    %% "tsec-hash-jca"          % tsecV,
        "io.github.jmcardon"    %% "tsec-hash-bouncy"       % tsecV,
        "io.github.jmcardon"    %% "tsec-jwt-mac"           % tsecV,
        "io.github.jmcardon"    %% "tsec-jwt-sig"           % tsecV,
        "io.github.jmcardon"    %% "tsec-http4s"            % tsecV,
        "com.avsystem.commons"  %% "commons-mongo"          % AVSystemCommonsVersion,
        "org.mongodb.scala"     %% "mongo-scala-driver"     % MongoScalaDriver,
        "co.fs2"                %% "fs2-reactive-streams"   % FS2Version,
        "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
        "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,
        compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
        "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
      ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % KindProjectorVersion),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % BetterMonadicForVersion),
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-Xfatal-warnings",
)
