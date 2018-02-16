
// Building both for JVM and JavaScript runtimes.

// To convince SBT not to publish any root level artifacts, I had a look at how scala-java-time does it.
// See https://github.com/cquiroz/scala-java-time/blob/master/build.sbt as a "template" for this build file.


val scalaVer = "2.12.4"
val crossScalaVer = Seq(scalaVer, "2.11.11", "2.13.0-M2")

lazy val commonSettings = Seq(
  name         := "yaidom",
  description  := "Extensible XML query API with multiple DOM-like implementations",
  organization := "eu.cdevreeze.yaidom",
  version      := "1.7.2-SNAPSHOT",

  scalaVersion       := scalaVer,
  crossScalaVersions := crossScalaVer,

  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint"),

  publishArtifact in Test := false,
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

  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.4" % "test"
  )
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

lazy val yaidom = crossProject.crossType(CrossType.Full).in(file("."))
  .settings(commonSettings: _*)
  .jvmSettings(
    libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "1.0.6",

    libraryDependencies += "org.scala-lang.modules" %%% "scala-java8-compat" % "0.8.0" % "optional",

    libraryDependencies += "junit" % "junit" % "4.12" % "test",

    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.5" % "test",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-M2" => Seq()
        case _           => Seq("org.scalameta" %%% "scalameta" % "2.0.1" % "test")
      }
    },

    libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test",

    libraryDependencies += "org.jdom" % "jdom" % "2.0.2" % "test",

    libraryDependencies += ("xom" % "xom" % "1.2.5" % "test").intransitive(),

    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.7.0-18" % "test",

    libraryDependencies += ("joda-time" % "joda-time" % "2.9.9" % "test").intransitive(),

    libraryDependencies += ("org.joda" % "joda-convert" % "1.8.1" % "test").intransitive(),

    libraryDependencies += "com.google.guava" % "guava" % "22.0" % "test",

    libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.2" % "test",

    libraryDependencies += ("com.fasterxml.woodstox" % "woodstox-core" % "5.0.3" % "test").intransitive(),

    libraryDependencies += "org.codehaus.woodstox" % "stax2-api" % "4.0.0" % "test",

    excludeFilter in (Compile, unmanagedSources) := {
      if (isBeforeJava8) {
        // Dangerous: the resulting JAR contents depends on the Java version!

        new SimpleFileFilter(_.toString.contains("java8"))
      } else {
        NothingFilter
      }
    },

    excludeFilter in (Test, unmanagedSources) := {
      if (isBeforeJava8) {
        // Exclude tests with Java 8 dependencies

        new SimpleFileFilter(f => f.toString.contains("java8") ||
          f.toString.contains("ScalaMetaExperimentTest") ||
          f.toString.contains("PackageDependencyTest") ||
          f.toString.contains("JvmIndependencyTest") ||
          f.toString.contains("NamePoolingTest"))
      } else if (scalaBinaryVersion.value == "2.13.0-M2") {
        new SimpleFileFilter(f => f.toString.contains("ScalaMetaExperimentTest") ||
          f.toString.contains("JvmIndependencyTest") ||
          f.toString.contains("PackageDependencyTest") ||
          f.toString.contains("JaxbTest"))
      } else if (isAtLeastJava9) {
        // Exclude tests with JAXB dependencies

        new SimpleFileFilter(f => f.toString.contains("JaxbTest"))
      } else {
        NothingFilter
      }
    }
  )
  .jsSettings(
    // Do we need this jsEnv?
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),

    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.4",

    parallelExecution in Test := false
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


// Helper functions

def isBeforeJava8: Boolean = {
  // Brittle
  scala.util.Try(Class.forName("java.util.stream.Stream")).toOption.isEmpty
}

def isAtLeastJava9: Boolean = {
  // Brittle
  scala.util.Try(Class.forName("javax.xml.catalog.Catalog")).toOption.nonEmpty
}
