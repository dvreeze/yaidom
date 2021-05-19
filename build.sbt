
// Building both for JVM and JavaScript runtimes.

// To convince SBT not to publish any root level artifacts, I had a look at how scala-java-time does it.
// See https://github.com/cquiroz/scala-java-time/blob/master/build.sbt as a "template" for this build file.


// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x

import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scalaVer = "3.0.0"
val crossScalaVer = Seq(scalaVer, "2.13.6")

ThisBuild / description  := "Extensible XML query API with multiple DOM-like implementations"
ThisBuild / organization := "eu.cdevreeze.yaidom"
ThisBuild / version      := "1.12.0-SNAPSHOT"

ThisBuild / scalaVersion       := scalaVer
ThisBuild / crossScalaVersions := crossScalaVer

ThisBuild / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case (Some((3, _))) =>
    Seq("-unchecked", "-source:3.0-migration")
  case _ =>
    Seq("-Wconf:cat=unused-imports:w,cat=unchecked:w,cat=deprecation:w,cat=feature:w,cat=lint:w", "-Ytasty-reader", "-Xsource:3")
})

ThisBuild / Test / publishArtifact := false
ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }

ThisBuild / pomExtra := pomData
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "2.0.0"

ThisBuild / libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.9" % Test

ThisBuild / libraryDependencies += "org.scalatestplus" %%% "scalacheck-1-15" % "3.2.9.0" % Test

lazy val root = project.in(file("."))
  .aggregate(yaidomJVM, yaidomJS)
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
  .jvmSettings(
    // By all means, override this version of Saxon if needed, possibly with a Saxon-EE release!

    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.9.1-8",

    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "2.9.0",

    libraryDependencies += "junit" % "junit" % "4.13.2" % Test,

    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.4" % Test,

    // JUnit tests (the ones intentionally written in Java) should run as well

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,

    libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % Test,

    libraryDependencies += "org.jdom" % "jdom2" % "2.0.6" % Test,

    libraryDependencies += ("xom" % "xom" % "1.3.7" % Test).intransitive(),

    libraryDependencies += ("com.fasterxml.woodstox" % "woodstox-core" % "6.2.6" % Test).intransitive(),

    libraryDependencies += "org.codehaus.woodstox" % "stax2-api" % "4.2.1" % Test,

    Compile / unmanagedSources / excludeFilter := (CrossVersion.partialVersion(scalaVersion.value) match {
      case (Some((3, _))) =>
        new SimpleFileFilter(f => f.toString.contains("ScalaXmlWrapperTest") ||
          f.toString.contains("DocumentParserTest") || f.toString.contains("XbrlInstanceQueryTest"))
      case _ =>
        NothingFilter
    }),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),

    mimaPreviousArtifacts := Set("eu.cdevreeze.yaidom" %%% "yaidom" % "1.11.0")
  )
  .jsConfigure(_.enablePlugins(TzdbPlugin))
  .jsSettings(
    // Add support for the DOM in `run` and `test`
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),

    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.3.0",

    // Hopefully for3Use2_13 soon not needed anymore
    libraryDependencies += ("org.scala-js" %%% "scalajs-dom" % "1.1.0").cross(CrossVersion.for3Use2_13),

    // Hopefully for3Use2_13 soon not needed anymore
    libraryDependencies += ("com.lihaoyi" %%% "scalatags" % "0.9.4" % Optional).cross(CrossVersion.for3Use2_13),

    Test / parallelExecution := false,

    mimaPreviousArtifacts := Set("eu.cdevreeze.yaidom" %%% "yaidom" % "1.11.0")
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
