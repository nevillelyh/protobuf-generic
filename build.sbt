val protobufVersion = sys.env.get("PROTO").getOrElse("3.17.2")
val isProto3 = protobufVersion.startsWith("3.")

val jacksonVersion = "2.12.3"
val jsr305Version = "3.0.2"
val scalaTestVersion = "3.2.9"

val commonSettings = Seq(
  organization := "me.lyh",
  scalaVersion := "2.13.6",
  crossScalaVersions := Seq("2.12.14", "2.13.6"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  Compile / doc / javacOptions := Seq("-source", "1.8"),
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
  Test / publishArtifact := false,
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
).settings(
  commonSettings ++ noPublishSettings,
  Compile / doc / sources := List(),
  Test / unmanagedSourceDirectories +=
    baseDirectory.value / "src" / "test" / s"proto-$protobufVersion",
  Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java" % protobufVersion
  )
).dependsOn(
  core
)

lazy val proto3Test: Project = Project(
  "proto3test",
  file("proto3test")
).settings(
  commonSettings ++ noPublishSettings,
  Test / unmanagedSourceDirectories +=
    baseDirectory.value / "src" / "test" / s"proto-$protobufVersion",
  Compile / doc / sources := List(),
  Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java" % protobufVersion
  ),
  if (isProto3) testProto3Settings else skipProto3Settings
).dependsOn(
  core
)

lazy val jmh: Project = Project(
  "jmh",
  file("jmh")
).enablePlugins(JmhPlugin)
  .settings(
    commonSettings,
    Jmh / sourceDirectory := (Test / sourceDirectory).value,
    Jmh / classDirectory := (Test / classDirectory).value,
    Jmh / dependencyClasspath := (Test / dependencyClasspath).value,
    // rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile'
    // (otherwise a clean 'jmh:run' would fail)
    Jmh / compile := (Jmh / compile).dependsOn(Test / compile).value,
    Jmh / run := (Jmh / run).dependsOn(Jmh / compile).evaluated,
    test := {}
  )
  .dependsOn(
    proto2Test % "test->test",
    proto3Test % "test->test"
  )

val testProto3Settings = Seq(
  libraryDependencies ++= Seq(
    "com.google.protobuf" % "protobuf-java-util" % protobufVersion % "test"
  )
)
val skipProto3Settings = Seq(
  test := {}
)
