val Http4sVersion        = "0.21.0-M5"
val CirceVersion         = "0.11.1"
val Specs2Version        = "4.1.0"
val LogbackVersion       = "1.2.3"
val MonixVersion         = "3.0.0-RC3"
val tsecV                = "0.2.0-M1"
val ReactiveMongoVersion = "0.18.6"

enablePlugins(
  JavaAppPackaging
)

lazy val root = (project in file("."))
  .settings(
    organization := "com.timesheet",
    name := "timesheet_app",
    version := "0.0.1",
    scalaVersion := "2.12.9",
    libraryDependencies ++= Seq(
      "org.http4s"         %% "http4s-blaze-server"      % Http4sVersion,
      "org.http4s"         %% "http4s-blaze-client"      % Http4sVersion,
      "org.http4s"         %% "http4s-circe"             % Http4sVersion,
      "org.http4s"         %% "http4s-dsl"               % Http4sVersion,
      "io.circe"           %% "circe-generic"            % CirceVersion,
      "org.specs2"         %% "specs2-core"              % Specs2Version % "test",
      "ch.qos.logback"     % "logback-classic"           % LogbackVersion,
      "io.monix"           %% "monix"                    % MonixVersion,
      "io.github.jmcardon" %% "tsec-common"              % tsecV,
      "io.github.jmcardon" %% "tsec-password"            % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-jca"          % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-bouncy"       % tsecV,
      "io.github.jmcardon" %% "tsec-mac"                 % tsecV,
      "io.github.jmcardon" %% "tsec-signatures"          % tsecV,
      "io.github.jmcardon" %% "tsec-hash-jca"            % tsecV,
      "io.github.jmcardon" %% "tsec-hash-bouncy"         % tsecV,
      "io.github.jmcardon" %% "tsec-jwt-mac"             % tsecV,
      "io.github.jmcardon" %% "tsec-jwt-sig"             % tsecV,
      "io.github.jmcardon" %% "tsec-http4s"              % tsecV,
      "org.reactivemongo"  %% "reactivemongo"            % ReactiveMongoVersion,
      "org.reactivemongo"  %% "reactivemongo-akkastream" % ReactiveMongoVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings"
)
