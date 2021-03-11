addSbtPlugin("com.github.sbt" % "sbt-protobuf" % "0.7.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.0.15")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.6")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.0")

// each protoc-jar 3.x release contains only 1 3.x.x binary
val protocJarVersion = sys.props("protobuf.version") match {
  case "3.10.0"     => "3.10.1"
  case "3.11.4" | _ => "3.11.4" // Latest protoc available
}

libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % protocJarVersion
)
