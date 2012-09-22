
// Keep in sync with the Maven pom.xml file!
// (I am not yet this far: https://github.com/sbt/sbt.github.com/blob/gen-master/src/jekyll/using_sonatype.md)
// Note that the sbt build is nice for day-to-day work, and the Maven build must currently be used for publishing artifacts.
// The sbt build uses the target directory in a different way than the Maven build, so, when switching
// between sbt and Maven, first do a clean (emptying the target directory).

name := "yaidom"

organization := "eu.cdevreeze.yaidom"

version := "0.6.2-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies += "net.jcip" % "jcip-annotations" % "1.0"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" % "test"

libraryDependencies += "org.jdom" % "jdom" % "2.0.2" % "test"

libraryDependencies += "xom" % "xom" % "1.2.5" % "test" intransitive()
