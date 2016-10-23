import sbtprotobuf.{ProtobufPlugin => PB}

val protobufVersion = Option(sys.props("protobuf.version")).getOrElse("3.1.0")
val protocVersion = Map("2.6.1" -> "-v261", "3.0.2" -> "-v310", "3.1.0" -> "-v310")(protobufVersion)
val isProto3 = protobufVersion.startsWith("3.")

val guavaVersion = "19.0"
val jacksonVersion = "2.8.3"
val jsr305Version = "3.0.1"
val paradiseVersion = "2.1.0"
val scalaCheckVersion = "1.13.2"
val scalaTestVersion = "3.0.0"

val commonSettings = PB.protobufSettings ++ Seq(
  organization := "me.lyh",

  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8"),
  scalacOptions ++= Seq("-target:jvm-1.7", "-deprecation", "-feature", "-unchecked"),
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked"),

  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java" % protobufVersion % "provided",
    "com.google.code.findbugs" % "jsr305" % jsr305Version % "provided",
    "com.google.guava" % "guava" % guavaVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  ),

  libraryDependencies ++= (
    if (scalaBinaryVersion.value == "2.10")
      Seq(compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full))
    else
      Nil
    ),

  version in PB.protobufConfig := protobufVersion,
  PB.runProtoc in PB.protobufConfig := (args =>
    com.github.os72.protocjar.Protoc.runProtoc(protocVersion  +: args.toArray)
  ),

  // Release settings
  releaseCrossBuild             := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle             := true,
  publishArtifact in Test       := false,
  sonatypeProfileName           := "me.lyh",
  pomExtra                      := {
    <url>https://github.com/spotify/ratatool</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com/nevillelyh/protobuf-generic.git</url>
      <connection>scm:git:git@github.com:nevillelyh/protobuf-generic.git</connection>
    </scm>
    <developers>
      <developer>
        <id>sinisa_lyh</id>
        <name>Neville Li</name>
        <url>https://twitter.com/sinisa_lyh</url>
      </developer>
    </developers>
  }
)

val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root: Project = Project(
  "root",
  file(".")
).settings(
  commonSettings ++ noPublishSettings
).aggregate(
  core,
  proto3Test
)

lazy val core: Project = Project(
  "protobuf-generic",
  file("core")
).settings(
  commonSettings,
  name := "protobuf-generic",
  description := "Generic protobuf manipulation"
)

lazy val proto3Test: Project = Project(
  "proto3test",
  file("proto3test")
).settings(
  commonSettings ++ noPublishSettings,
  if (isProto3) proto3Settings else noProto3Settings
).dependsOn(
  core
)

val proto3Settings = Seq(
  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java-util" % protobufVersion % "test"
  )
)
val noProto3Settings = Seq(
  test := {}
)
