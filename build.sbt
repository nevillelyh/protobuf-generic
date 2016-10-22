import sbtprotobuf.{ProtobufPlugin => PB}

organization := "me.lyh"
name := "protobuf-generic"
description := "Generic protobuf manipulation"

scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.10.6", "2.11.8")
scalacOptions ++= Seq("-target:jvm-1.7", "-deprecation", "-feature", "-unchecked")
javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked")

val protobufVersion = Option(sys.props("protobuf.version")).getOrElse("3.1.0")
val protocVersion = Map("2.6.1" -> "-v261", "3.0.2" -> "-v310", "3.1.0" -> "-v310")(protobufVersion)

val guavaVersion = "19.0"
val jacksonVersion = "2.8.3"
val jsr305Version = "3.0.1"
val paradiseVersion = "2.1.0"
val scalaCheckVersion = "1.13.2"
val scalaTestVersion = "3.0.0"

libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % protobufVersion % "provided",
  "com.google.code.findbugs" % "jsr305" % jsr305Version % "provided",
  "com.google.guava" % "guava" % guavaVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)

libraryDependencies ++= (
  if (scalaBinaryVersion.value == "2.10")
    Seq(compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full))
  else
    Nil
  )

PB.protobufSettings
version in PB.protobufConfig := protobufVersion
PB.runProtoc in PB.protobufConfig := (args =>
  com.github.os72.protocjar.Protoc.runProtoc(protocVersion  +: args.toArray)
)

// Release settings
releaseCrossBuild             := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishMavenStyle             := true
publishArtifact in Test       := false
sonatypeProfileName           := "me.lyh"
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
