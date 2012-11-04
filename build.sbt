
// Keep in sync with the Maven pom.xml file!
// (I am not yet this far: https://github.com/sbt/sbt.github.com/blob/gen-master/src/jekyll/using_sonatype.md)
// Note that the sbt build is nice for day-to-day work, and the Maven build must currently be used for publishing artifacts.
// The sbt build uses the target directory in a different way than the Maven build, so, when switching
// between sbt and Maven, first do a clean (emptying the target directory).

name := "yaidom"

organization := "eu.cdevreeze.yaidom"

version := "0.6.3-SNAPSHOT"

scalaVersion := "2.9.1"

crossScalaVersions := Seq("2.9.2", "2.9.1", "2.9.0-1", "2.9.0", "2.10.0-RC1")

scalacOptions ++= Seq("-unchecked", "-deprecation")

scalacOptions <++= scalaBinaryVersion map { version =>
  if (version.contains("2.10")) Seq("-feature") else Seq()
}

sources in Test <++= scalaBinaryVersion map { version =>
  val newSources = new java.io.File("src/test-reflect/scala/eu/cdevreeze/yaidom/reflect").listFiles.toSeq
  if (version.contains("2.10")) newSources else Seq()
}

libraryDependencies += "net.jcip" % "jcip-annotations" % "1.0"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies <+= scalaBinaryVersion { version =>
  if (version.contains("2.10"))
    "org.scalatest" % "scalatest_2.10.0-RC1" % "1.8-2.10.0-RC1-B1" % "test"
  else
    "org.scalatest" %% "scalatest" % "1.8" % "test"
}

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
