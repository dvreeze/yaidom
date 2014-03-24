
// Keep in sync with the Maven pom.xml file!
// See http://www.scala-sbt.org/release/docs/Community/Using-Sonatype.html for how to publish to
// Sonatype, using sbt only.

name := "yaidom"

organization := "eu.cdevreeze.yaidom"

version := "0.8.1-SNAPSHOT"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.0-RC3")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

(unmanagedSourceDirectories in Compile) <++= scalaBinaryVersion apply { version =>
  val newSourceDirs = Seq() // Not yet used
  if (version.contains("2.11")) newSourceDirs else Seq()
}

(unmanagedSourceDirectories in Test) <++= scalaBinaryVersion apply { version =>
  val newSourceDirs = Seq() // Not yet used
  if (version.contains("2.11")) newSourceDirs else Seq()
}

libraryDependencies <++= scalaBinaryVersion apply { version =>
  if (version.contains("2.11")) Seq("org.scala-lang.modules" % "scala-xml_2.11.0-RC3" % "1.0.1")
  else Seq()
}

libraryDependencies <++= scalaBinaryVersion apply { version =>
  if (version.contains("2.11")) Seq("org.scala-lang.modules" % "scala-parser-combinators_2.11.0-RC3" % "1.0.1")
  else Seq()
}

libraryDependencies += "net.jcip" % "jcip-annotations" % "1.0"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.2.1"

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies <+= scalaBinaryVersion apply { version =>
  if (version.contains("2.11")) "org.scalatest" % "scalatest_2.11.0-RC3" % "2.1.2" % "test"
  else "org.scalatest" %% "scalatest" % "2.1.2" % "test"
}

libraryDependencies <+= scalaBinaryVersion apply { version =>
  if (version.contains("2.11")) "org.scalacheck" % "scalacheck_2.11.0-RC3" % "1.11.3" % "test"
  else "org.scalacheck" %% "scalacheck" % "1.11.3" % "test"
}

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test"

libraryDependencies += "org.jdom" % "jdom" % "2.0.2" % "test"

libraryDependencies += ("xom" % "xom" % "1.2.5" % "test").intransitive()

libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.5.1-4" % "test"

libraryDependencies += ("joda-time" % "joda-time" % "2.3" % "test").intransitive()

libraryDependencies += ("org.joda" % "joda-convert" % "1.2" % "test").intransitive()

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
