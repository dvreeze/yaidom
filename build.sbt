
// Keep in sync with the Maven pom.xml file!
// Note that the sbt build is nice for day-to-day work, and the Maven build is nice for deployments.
// The sbt build uses the target directory in a different way than the Maven build, so, when switching
// between sbt and Maven, first do a clean (emptying the target directory).

name := "yaidom"

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
