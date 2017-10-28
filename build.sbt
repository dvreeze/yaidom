
lazy val root = project.in(file(".")).
  aggregate(yaidomJS, yaidomJVM).
  settings(
    name := "yaidom",

    organization := "eu.cdevreeze.yaidom",

    // Seems superfluous at root level, but guards against unresolved dependencies
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.12.4", "2.11.11", "2.13.0-M2"),

    // Trying to switch off publishing for root artifacts (the ones other than in the jvm and js projects)

    publish := {},
    publishLocal := {},

    publishArtifact := false,

    // Additionally, see https://stackoverflow.com/questions/8786708/how-to-disable-package-and-publish-tasks-for-root-aggregate-module-in-multi-modu

    packageBin := { new File("") },
    packageDoc := { new File("") },
    packageSrc := { new File("") }
  )


// The jvm and js projects

lazy val yaidom = crossProject.in(file(".")).
  settings(
    version := "1.6.5-SNAPSHOT",

    scalaVersion := "2.12.4",

    crossScalaVersions := Seq("2.12.4", "2.11.11", "2.13.0-M2"),

    // See: Toward a safer Scala
    // http://downloads.typesafe.com/website/presentations/ScalaDaysSF2015/Toward%20a%20Safer%20Scala%20@%20Scala%20Days%20SF%202015.pdf

    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint"),

    // resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",

    // addCompilerPlugin("com.artima.supersafe" %%% "supersafe" % "1.0.3"),

    publishMavenStyle := true,

    publishTo := {
      val vers = version.value

      val nexus = "https://oss.sonatype.org/"

      if (vers.trim.endsWith("SNAPSHOT")) {
        Some("snapshots" at nexus + "content/repositories/snapshots")
      } else {
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      }
    },

    publishArtifact in Test := false,

    pomIncludeRepository := { repo => false },

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
  ).
  jvmSettings(
    libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "1.0.6",

    libraryDependencies += "org.scala-lang.modules" %%% "scala-java8-compat" % "0.8.0" % "optional",

    libraryDependencies += "junit" % "junit" % "4.12" % "test",

    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.4" % "test",

    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.13.5" % "test",

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-M2" => Seq()
        case _           => Seq("org.scalameta" %%% "scalameta" % "1.8.0" % "test")
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
      val base = baseDirectory.value

      if (isBeforeJava8) {
        new SimpleFileFilter(_.toString.contains("java8"))
      } else {
        NothingFilter
      }
    },

    excludeFilter in (Test, unmanagedSources) := {
      val base = baseDirectory.value

      if (isBeforeJava8) {
        // Exclude tests with Java 8 dependencies

        new SimpleFileFilter(f => f.toString.contains("java8") ||
          f.toString.contains("ScalaMetaExperimentTest") ||
          f.toString.contains("PackageDependencyTest") ||
          f.toString.contains("NamePoolingTest"))
      } else if (scalaBinaryVersion.value == "2.13.0-M2") {
        new SimpleFileFilter(f => f.toString.contains("ScalaMetaExperimentTest") ||
          f.toString.contains("PackageDependencyTest"))
      } else {
        NothingFilter
      }
    }
  ).
  jsSettings(
    crossScalaVersions := Seq("2.12.4", "2.11.11"),

    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "2.13.0-M2" => Seq()
        case _           => Seq("org.scala-js" %%% "scalajs-dom" % "0.9.2")
      }
    }
  )


lazy val yaidomJVM = yaidom.jvm
lazy val yaidomJS = yaidom.js


// Helper functions

def isBeforeJava8: Boolean = {
  // Brittle
  scala.util.Try(Class.forName("java.util.stream.Stream")).toOption.isEmpty
}
