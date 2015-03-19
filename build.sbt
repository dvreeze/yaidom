
// Keep in sync with the Maven pom.xml file!
// See http://www.scala-sbt.org/release/docs/Community/Using-Sonatype.html for how to publish to
// Sonatype, using sbt only.

name := "yaidom"

organization := "eu.cdevreeze.yaidom"

version := "1.3.5"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.11.4", "2.10.4")

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
  if (version.contains("2.11")) Seq("org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2")
  else Seq()
}

libraryDependencies += "net.jcip" % "jcip-annotations" % "1.0"

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies <+= scalaBinaryVersion apply { version =>
  if (version.contains("2.11")) "org.scalatest" % "scalatest_2.11" % "2.1.3" % "test"
  else "org.scalatest" %% "scalatest" % "2.2.2" % "test"
}

libraryDependencies <+= scalaBinaryVersion apply { version =>
  if (version.contains("2.11")) "org.scalacheck" % "scalacheck_2.11" % "1.11.3" % "test"
  else "org.scalacheck" %% "scalacheck" % "1.11.6" % "test"
}

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test"

libraryDependencies += "org.jdom" % "jdom" % "2.0.2" % "test"

libraryDependencies += ("xom" % "xom" % "1.2.5" % "test").intransitive()

libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.5.1-8" % "test"

libraryDependencies += ("joda-time" % "joda-time" % "2.5" % "test").intransitive()

libraryDependencies += ("org.joda" % "joda-convert" % "1.7" % "test").intransitive()

libraryDependencies += "com.google.guava" % "guava" % "18.0" % "test"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.0" % "test"

libraryDependencies += ("org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1" % "test").intransitive()

libraryDependencies += "org.codehaus.woodstox" % "stax2-api" % "3.1.4" % "test"

// resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

// addCompilerPlugin("com.artima.supersafe" %% "supersafe" % "1.0.3")

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
