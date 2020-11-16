val protobufVersion = sys.env.get("PROTO").getOrElse("3.14.0")
val isProto3 = protobufVersion.startsWith("3.")

val jacksonVersion = "2.11.3"
val jsr305Version = "3.0.2"
val scalaTestVersion = "3.2.3"

val commonSettings = Seq(
  organization := "me.lyh",
  scalaVersion := "2.13.3",
  crossScalaVersions := Seq("2.12.12", "2.13.3"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  javacOptions in (Compile, doc) := Seq("-source", "1.8"),
  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java" % protobufVersion % Provided,
    "com.google.code.findbugs" % "jsr305" % jsr305Version % Provided,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  ),
  // Release settings
  publishTo := Some(
    if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging
  ),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  sonatypeProfileName := "me.lyh",
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/nevillelyh/protobuf-generic")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/nevillelyh/protobuf-generic.git"),
      "scm:git:git@github.com:nevillelyh/protobuf-generic.git"
    )
  ),
  developers := List(
    Developer(
      id = "sinisa_lyh",
      name = "Neville Li",
      email = "neville.lyh@gmail.com",
      url = url("https://twitter.com/sinisa_lyh")
    )
  )
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
  "protobuf-generic-parent",
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
).enablePlugins(ProtobufPlugin)
  .settings(
    commonSettings ++ protoSettings ++ noPublishSettings,
    Compile / doc / sources := List(),
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    protobufProtocOptions in ProtobufConfig ++= Seq("--include_std_types")
  )
  .dependsOn(
    core
  )

lazy val proto3Test: Project = Project(
  "proto3test",
  file("proto3test")
).enablePlugins(ProtobufPlugin)
  .settings(
    commonSettings ++ protoSettings ++ noPublishSettings,
    Compile / doc / sources := List(),
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    protobufProtocOptions in ProtobufConfig ++= Seq("--include_std_types"),
    if (isProto3) proto3Settings else noProto3Settings
  )
  .dependsOn(
    core
  )

lazy val jmh: Project = Project(
  "jmh",
  file("jmh")
).enablePlugins(JmhPlugin)
  .settings(
    commonSettings ++ noProto3Settings,
    sourceDirectory in Jmh := (sourceDirectory in Test).value,
    classDirectory in Jmh := (classDirectory in Test).value,
    dependencyClasspath in Jmh := (dependencyClasspath in Test).value,
    // rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile'
    // (otherwise a clean 'jmh:run' would fail)
    compile in Jmh := (compile in Jmh).dependsOn(compile in Test).value,
    run in Jmh := (run in Jmh).dependsOn(compile in Jmh).evaluated
  )
  .dependsOn(
    proto2Test % "test->test",
    proto3Test % "test->test"
  )

val proto3Settings = Seq(
  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java-util" % protobufVersion % "test"
  )
)
val noProto3Settings = Seq(
  test := {}
)
