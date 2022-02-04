

// For a list of well-known plugins, see https://www.scala-sbt.org/1.x/docs/Community-Plugins.html.

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.5.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.7.3")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

// See https://github.com/albuch/sbt-dependency-check (the plugin checks potential security vulnerabilities)
// Tasks: dependencyCheck, dependencyCheckAggregate, etc.
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "3.1.2")

// See https://github.com/rtimush/sbt-updates
// Tasks: dependencyUpdates, dependencyUpdatesReport
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.3")

// See https://github.com/cb372/sbt-explicit-dependencies (like maven-dependency-plugin:analyze)
// Tasks: undeclaredCompileDependencies, undeclaredCompileDependenciesTest, unusedCompileDependencies etc.

// Plugin sbt-explicit-dependencies 0.2.13 does not work with sbt 1.4.0.
// With sbt 1.4.0: "java.lang.ClassCastException: class xsbti.BasicVirtualFileRef cannot be cast to class java.io.File"
// With sbt-explicit-dependencies 0.2.14 we don't have that issue, but other issues (elsewhere) remain. So using sbt 1.3.13.
// The sbt-explicit-dependencies plugin also does not work well with cross-platform builds, it seems.
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.16")

addSbtPlugin("ch.epfl.scala" % "sbt-missinglink" % "0.3.2")

// See https://github.com/sbt/sbt-duplicates-finder (finds duplicates at level of classes etc.)
// Should detect Saxon-HE and Saxon-EE together on classpath
// Tasks: checkDuplicates, checkDuplicatesTest
addSbtPlugin("com.github.sbt" % "sbt-duplicates-finder" % "1.1.0")

addSbtPlugin("io.github.cquiroz" % "sbt-tzdb" % "1.0.1")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"

// See https://scalacenter.github.io/scalafix/docs/users/installation.html
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.34")
