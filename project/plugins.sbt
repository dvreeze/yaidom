
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")

addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.18")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")
// sbt-scalafmt requires Java 8, so does not work in a Java 7 build
// addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")

