addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.6.5")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2-1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.5")

// each protoc-jar 3.x release contains only 1 3.x.x binary
val protocJarVersion = sys.props("protobuf.version") match {
  case "3.6.0" | null => "3.6.0"
  case "3.5.1" => "3.5.1.1"
  case "3.3.0" | _ => "3.3.0.1"
}

libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % protocJarVersion
)
