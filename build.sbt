val protobufVersion = Option(sys.props("protobuf.version")).getOrElse("3.4.0")
val isProto3 = protobufVersion.startsWith("3.")

val jacksonVersion = "2.8.8"
val jsr305Version = "3.0.1"
val paradiseVersion = "2.1.0"
val scalaTestVersion = "3.0.3"

def jdkVersion(scalaBinaryVersion: String) = if (scalaBinaryVersion == "2.12") "1.8" else "1.7"

val commonSettings = Seq(
  organization := "me.lyh",

  scalaVersion := "2.12.3",
  crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.3"),
  scalacOptions ++= Seq("-target:jvm-" + jdkVersion(scalaBinaryVersion.value), "-deprecation", "-feature", "-unchecked"),
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked"),

  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java" % protobufVersion % "provided",
    "com.google.code.findbugs" % "jsr305" % jsr305Version % "provided",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  ),

  // Release settings
  releaseCrossBuild             := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle             := true,
  publishArtifact in Test       := false,
  sonatypeProfileName           := "me.lyh",
  pomExtra                      := {
    <url>https://github.com/nevillelyh/protobuf-generic</url>
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

val protoSettings = Seq(
  version in ProtobufConfig := protobufVersion,
  protobufRunProtoc in ProtobufConfig := (args =>
    com.github.os72.protocjar.Protoc.runProtoc(s"-v$protobufVersion" +: args.toArray)
  )
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
  proto2Test,
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

lazy val proto2Test: Project = Project(
  "proto2test",
  file("proto2test")
).enablePlugins(ProtobufPlugin).settings(
  commonSettings ++ protoSettings ++ noPublishSettings
).dependsOn(
  core
)
lazy val proto3Test: Project = Project(
  "proto3test",
  file("proto3test")
).enablePlugins(ProtobufPlugin).settings(
  commonSettings ++ protoSettings ++ noPublishSettings,
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
