
// Building both for JVM and JavaScript runtimes.

// To convince SBT not to publish any root level artifacts, I had a look at how scala-java-time does it.
// See https://github.com/cquiroz/scala-java-time/blob/master/build.sbt as a "template" for this build file.


// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x

import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scalaVer = "2.13.0-M4"
val crossScalaVer = Seq(scalaVer)

lazy val commonSettings = Seq(
  name         := "yaidom",
  description  := "Extensible XML query API with multiple DOM-like implementations",
  organization := "eu.cdevreeze.yaidom",
  version      := "1.9.0",

  scalaVersion       := scalaVer,
  crossScalaVersions := crossScalaVer,

  // No longer targeting Java 6 class files, and not only to prevent compilation errors like:
  // "Static methods in interface require -target:jvm-1.8".

  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint", "-target:jvm-1.8"),

  libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "1.1.0",

  libraryDependencies ++= {
    scalaBinaryVersion.value match {
      case "2.13.0-M4" => Seq("org.scalatest" %%% "scalatest" % "3.0.6-SNAP2" % "test")
      case _           => Seq("org.scalatest" %%% "scalatest" % "3.0.5" % "test")
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

    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.8.0-14",

    libraryDependencies += "org.scala-lang.modules" %%% "scala-java8-compat" % "0.9.0",

    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "2.6.2",

    libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.2",

    libraryDependencies += "junit" % "junit" % "4.12" % "test",

    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.0" % "test",

    // JUnit tests (the ones intentionally written in Java) should run as well

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-M4" => Seq()
        case _           => Seq("org.scalameta" %%% "scalameta" % "3.7.4" % "test")
      }
    },

    libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test",

    libraryDependencies += "org.jdom" % "jdom" % "2.0.2" % "test",

    libraryDependencies += ("xom" % "xom" % "1.2.5" % "test").intransitive(),

    libraryDependencies += ("com.fasterxml.woodstox" % "woodstox-core" % "5.1.0" % "test").intransitive(),

    libraryDependencies += "org.codehaus.woodstox" % "stax2-api" % "4.1" % "test",

    // Excluding UpdateTest in Scala 2.13.0-M4 build due to regression:
    // "inferred type ... contains type selection from volatile type ..."

    excludeFilter in (Test, unmanagedSources) := {
      if (scalaBinaryVersion.value == "2.13.0-M4") {
        new SimpleFileFilter(f => f.toString.contains("ScalaMetaExperimentTest") ||
          f.toString.contains("JvmIndependencyTest") ||
          f.toString.contains("PackageDependencyTest") ||
          f.toString.contains("UpdateTest") ||
          f.toString.contains("JaxbTest"))
      } else if (isAtLeastJava9) {
        // Exclude tests with JAXB dependencies

        new SimpleFileFilter(f => f.toString.contains("JaxbTest"))
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
        case "2.13.0-M4" => Seq()
        case _           =>
          Seq(
            "com.zoepepper" %%% "scalajs-jsjoda" % "1.1.1",
            "com.zoepepper" %%% "scalajs-jsjoda-as-java-time" % "1.1.1")
      }
    },

    jsDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-M4" => Seq()
        case _           =>
          Seq(
            "org.webjars.npm" % "js-joda" % "1.3.0" / "dist/js-joda.js" minified "dist/js-joda.min.js",
            "org.webjars.npm" % "js-joda-timezone" % "1.0.0" / "dist/js-joda-timezone.js" minified "dist/js-joda-timezone.min.js")
      }
    },

    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.6",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-M4" => Seq()
        case _           => Seq("com.lihaoyi" %%% "scalatags" % "0.6.7" % "optional")
      }
    },

    parallelExecution in Test := false,

    excludeFilter in (Compile, unmanagedSources) := {
      if (scalaBinaryVersion.value == "2.13.0-M4") {
        new SimpleFileFilter(f => f.toString.contains("jsdemoapp"))
      } else {
        NothingFilter
      }
    },

    mimaPreviousArtifacts := Set("eu.cdevreeze.yaidom" %%% "yaidom" % "1.8.1")
  )

lazy val yaidomJVM = yaidom.jvm
lazy val yaidomJS = yaidom.js

// Helper functions

def isAtLeastJava9: Boolean = {
  // Brittle
  scala.util.Try(Class.forName("javax.xml.catalog.Catalog")).toOption.nonEmpty
}
