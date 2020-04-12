addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.6.5")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.4")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.7")

// each protoc-jar 3.x release contains only 1 3.x.x binary
val protocJarVersion = sys.props("protobuf.version") match {
  case "3.10.0" | null => "3.10.1"
  case "3.9.2"         => "3.9.2"
  case "3.8.0"         => "3.8.0"
  case "3.7.1"         => "3.7.1"
  case "3.6.0"         => "3.6.0"
  case "3.5.1"         => "3.5.1.1"
  case "3.3.0" | _     => "3.3.0.1"
}

libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % protocJarVersion
)
