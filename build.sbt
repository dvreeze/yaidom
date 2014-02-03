
// Keep in sync with the Maven pom.xml file!
// See http://www.scala-sbt.org/release/docs/Community/Using-Sonatype.html for how to publish to
// Sonatype, using sbt only.

name := "yaidom"

organization := "eu.cdevreeze.yaidom"

version := "0.8.0"

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.10.3")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

(unmanagedSourceDirectories in Compile) <++= scalaBinaryVersion apply { version =>
  val newSourceDirs = Seq() // Not yet used
  if (version.contains("2.11")) newSourceDirs else Seq()
}

(unmanagedSourceDirectories in Test) <++= scalaBinaryVersion apply { version =>
  val newSourceDirs = Seq() // Not yet used
  if (version.contains("2.11")) newSourceDirs else Seq()
}

libraryDependencies += "net.jcip" % "jcip-annotations" % "1.0"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.2.1"

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.0" % "test"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test"

libraryDependencies += "org.jdom" % "jdom" % "2.0.2" % "test"

libraryDependencies += ("xom" % "xom" % "1.2.5" % "test").intransitive()

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { repo => false }

pomExtra := {
  <url>https://github.com/dvreeze/yaidom</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
      <comments>Yaidom is licensed under Apache License, Version 2.0</comments>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git@github.com:dvreeze/yaidom.git</connection>
    <url>https://github.com/dvreeze/yaidom.git</url>
    <developerConnection>scm:git:git@github.com:dvreeze/yaidom.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>dvreeze</id>
      <name>Chris de Vreeze</name>
      <email>chris.de.vreeze@caiway.net</email>
    </developer>
  </developers>
}
