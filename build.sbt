
// Building both for JVM and JavaScript runtimes.

// To convince SBT not to publish any root level artifacts, I had a look at how scala-java-time does it.
// See https://github.com/cquiroz/scala-java-time/blob/master/build.sbt as a "template" for this build file.


// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x

import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scalaVer = "2.13.2"
val crossScalaVer = Seq(scalaVer, "2.12.11")

lazy val commonSettings = Seq(
  name         := "yaidom",
  description  := "Extensible XML query API with multiple DOM-like implementations",
  organization := "eu.cdevreeze.yaidom",
  version      := "1.11.0-SNAPSHOT",

  scalaVersion       := scalaVer,
  crossScalaVersions := crossScalaVer,

  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint", "-target:jvm-1.8"),

  Test / publishArtifact := false,
  publishMavenStyle := true,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

  pomExtra := pomData,
  pomIncludeRepository := { _ => false },

  libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "2.0.0-M1",

  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.1.1" % "test",

  libraryDependencies += "org.scalatestplus" %%% "scalacheck-1-14" % "3.1.1.1" % "test"
)

lazy val root = project.in(file("."))
  .aggregate(yaidomJVM, yaidomJS)
  .settings(commonSettings: _*)
  .settings(
    name                 := "yaidom",
    // Thanks, scala-java-time, for showing us how to prevent any publishing of root level artifacts:
    // No, SBT, we don't want any artifacts for root. No, not even an empty jar.
    publish              := {},
    publishLocal         := {},
    publishArtifact      := false,
    Keys.`package`       := file(""))

lazy val yaidom = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .settings(commonSettings: _*)
  .jvmSettings(
    // By all means, override this version of Saxon if needed, possibly with a Saxon-EE release!

    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.9.1-7",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13" => Seq()
        case _      => Seq("org.scala-lang.modules" %%% "scala-java8-compat" % "0.9.0")
      }
    },

    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "2.8.0",

    libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.2",

    libraryDependencies += "junit" % "junit" % "4.12" % "test",

    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.3" % "test",

    // JUnit tests (the ones intentionally written in Java) should run as well

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13" => Seq()
        case _      => Seq("org.scalameta" %%% "scalameta" % "4.2.3" % "test")
      }
    },

    libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test",

    libraryDependencies += "org.jdom" % "jdom2" % "2.0.6" % "test",

    libraryDependencies += ("xom" % "xom" % "1.3.2" % "test").intransitive(),

    libraryDependencies += ("com.fasterxml.woodstox" % "woodstox-core" % "6.0.1" % "test").intransitive(),

    libraryDependencies += "org.codehaus.woodstox" % "stax2-api" % "4.2" % "test",

    Compile / unmanagedSourceDirectories += {
      val sourceDir = (Compile / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => sourceDir / "scala-2.13+"
        case _                       => sourceDir / "scala-2.13-"
      }
    },

    Test / unmanagedSourceDirectories += {
      val sourceDir = (Test / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => sourceDir / "scala-2.13+"
        case _                       => sourceDir / "scala-2.13-"
      }
    },

    Test / unmanagedSources / excludeFilter := {
      if (scalaBinaryVersion.value == "2.13") {
        new SimpleFileFilter(f =>
          f.toString.contains("java8") ||
          f.toString.contains("DomInteropTest") || // Serialization issue since Scala 2.13.0-M5?
          f.toString.contains("DomLSInteropTest")) // Serialization issue since Scala 2.13.0-M5?
      } else {
        NothingFilter
      }
    },

    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),

    mimaPreviousArtifacts := Set("eu.cdevreeze.yaidom" %%% "yaidom" % "1.10.3")
  )
  .jsConfigure(_.enablePlugins(TzdbPlugin))
  .jsSettings(
    // Add support for the DOM in `run` and `test`
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),

    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0",

    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.0.0",

    libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.8.6" % "optional",

    Test / parallelExecution := false,

    Compile / unmanagedSources / excludeFilter := {
      if (scalaBinaryVersion.value == "2.13") {
        new SimpleFileFilter(f => f.toString.contains("java8") || f.toString.contains("jsdemoapp"))
      } else {
        NothingFilter
      }
    },

    mimaPreviousArtifacts := Set("eu.cdevreeze.yaidom" %%% "yaidom" % "1.10.3")
  )

lazy val yaidomJVM = yaidom.jvm
lazy val yaidomJS = yaidom.js

lazy val pomData =
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
