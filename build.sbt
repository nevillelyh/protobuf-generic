val protobufVersion = sys.env.get("PROTO").filterNot(_.isEmpty).getOrElse("3.24.4")
val isProto3 = protobufVersion.startsWith("3.")

val jacksonVersion = "2.15.3"
val jsr305Version = "3.0.2"
val scalaTestVersion = "3.2.17"

val commonSettings = Seq(
  organization := "me.lyh",
  scalaVersion := "2.13.12",
  crossScalaVersions := Seq("2.12.18", "2.13.12"),
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
    if (isSnapshot.value) Opts.resolver.sonatypeOssSnapshots.head else Opts.resolver.sonatypeStaging
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

lazy val protoc = taskKey[Seq[File]]("protoc")

lazy val protocSettings = Seq(
  Test / protoc / target := (Test / sourceManaged).value / "compiled_protobuf",
  Test / protoc := {
    val pwd = (ThisBuild / baseDirectory).value
    val sh = pwd / "protoc.sh"
    val src = ((Test / sourceDirectory).value / "protobuf" ** "*.proto").get().map(_.toString)
    val dst = (Test / protoc / target).value

    val logger = ConsoleLogger()
    logger.info(s"compiling Protobuf to $dst")
    src.foreach(logger.info(_))

    dst.mkdirs()
    val cmd = Seq(sh.toString, protobufVersion, s"-I$pwd", s"--java_out=$dst") ++ src
    import scala.sys.process._
    val err = new StringBuilder
    // workaround for race condition in protoc.sh
    System.out.synchronized {
      val p = cmd.run(ProcessLogger(l => (), l => err ++= l))
      if (p.exitValue != 0) {
        throw new RuntimeException(err.toString())
      }
    }
    (dst ** "*.java").get()
  },
  Test / sourceGenerators += (Test / protoc).taskValue
)

lazy val proto2Test: Project = Project(
  "proto2test",
  file("proto2test")
).settings(
  commonSettings ++ protocSettings ++ noPublishSettings,
  Compile / doc / sources := List(),
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
  commonSettings ++ protocSettings ++ noPublishSettings,
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
