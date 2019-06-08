
// Building both for JVM and JavaScript runtimes.

// To convince SBT not to publish any root level artifacts, I had a look at how scala-java-time does it.
// See https://github.com/cquiroz/scala-java-time/blob/master/build.sbt as a "template" for this build file.


// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x

import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scalaVer = "2.12.8"
val crossScalaVer = Seq(scalaVer, "2.13.0-RC3")

lazy val commonSettings = Seq(
  name         := "yaidom",
  description  := "Extensible XML query API with multiple DOM-like implementations",
  organization := "eu.cdevreeze.yaidom",
  version      := "1.9.1-SNAPSHOT",

  scalaVersion       := scalaVer,
  crossScalaVersions := crossScalaVer,

  // No longer targeting Java 6 class files, and not only to prevent compilation errors like:
  // "Static methods in interface require -target:jvm-1.8".

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

  libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "1.2.0",

  libraryDependencies ++= {
    scalaBinaryVersion.value match {
      case "2.13.0-RC3" => Seq("org.scalatest" %%% "scalatest" % "3.1.0-SNAP12" % "test")
      case _            => Seq("org.scalatest" %%% "scalatest" % "3.0.7" % "test")
    }
  },

  libraryDependencies ++= {
    scalaBinaryVersion.value match {
      case "2.13.0-RC3" => Seq("org.scalatestplus" %%% "scalatestplus-scalacheck" % "1.0.0-SNAP7" % "test")
      case _            => Seq("org.scalatestplus" %%% "scalatestplus-scalacheck" % "1.0.0-SNAP7" % "test")
    }
  }
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

    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.9.1-3",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-RC3" => Seq()
        case _            => Seq("org.scala-lang.modules" %%% "scala-java8-compat" % "0.9.0")
      }
    },

    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "2.7.0",

    libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.2",

    libraryDependencies += "junit" % "junit" % "4.12" % "test",

    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.0" % "test",

    // JUnit tests (the ones intentionally written in Java) should run as well

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-RC3" => Seq()
        case _            => Seq("org.scalameta" %%% "scalameta" % "4.1.11" % "test")
      }
    },

    libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test",

    libraryDependencies += "org.jdom" % "jdom" % "2.0.2" % "test",

    libraryDependencies += ("xom" % "xom" % "1.3.2" % "test").intransitive(),

    libraryDependencies += ("com.fasterxml.woodstox" % "woodstox-core" % "5.2.1" % "test").intransitive(),

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
      if (scalaBinaryVersion.value == "2.13.0-RC3") {
        new SimpleFileFilter(f =>
          f.toString.contains("java8") ||
          f.toString.contains("DomInteropTest") || // Serialization issue since Scala 2.13.0-M5?
          f.toString.contains("DomLSInteropTest")) // Serialization issue since Scala 2.13.0-M5?
      } else {
        NothingFilter
      }
    },

    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),

    mimaPreviousArtifacts := Set("eu.cdevreeze.yaidom" %%% "yaidom" % "1.8.1")
  )
  .jsSettings(
    // Do we need this jsEnv?
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),

    // It turns out that scalajs-jsjoda is far more complete than scalajs-java-time!

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-RC3" => Seq()
        case _            =>
          Seq(
            "com.zoepepper" %%% "scalajs-jsjoda" % "1.1.2",
            "com.zoepepper" %%% "scalajs-jsjoda-as-java-time" % "1.1.2")
      }
    },

    jsDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-RC3" => Seq()
        case _            =>
          Seq(
            "org.webjars.npm" % "js-joda" % "1.3.0" / "dist/js-joda.js" minified "dist/js-joda.min.js",
            "org.webjars.npm" % "js-joda-timezone" % "1.0.0" / "dist/js-joda-timezone.js" minified "dist/js-joda-timezone.min.js")
      }
    },

    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.7",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-RC3" => Seq()
        case _            => Seq("com.lihaoyi" %%% "scalatags" % "0.6.8" % "optional")
      }
    },

    Test / parallelExecution := false,

    Compile / unmanagedSources / excludeFilter := {
      if (scalaBinaryVersion.value == "2.13.0-RC3") {
        new SimpleFileFilter(f => f.toString.contains("java8") || f.toString.contains("jsdemoapp"))
      } else {
        NothingFilter
      }
    },

    mimaPreviousArtifacts := Set("eu.cdevreeze.yaidom" %%% "yaidom" % "1.8.1")
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
