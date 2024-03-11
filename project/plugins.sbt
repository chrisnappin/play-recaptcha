// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.2")

// The PGP plugin (for signing sonatype releases)
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

// The scoverage plugin (measures statement coverage for unit tests)
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.11")

// Shows an ascii library dependency graph (run dependencyTree)
addDependencyTreePlugin