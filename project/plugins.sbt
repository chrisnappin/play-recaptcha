// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.1")

// The PGP plugin (for signing sonatype releases)
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

// The scoverage plugin (measures statement coverage for unit tests)
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "2.0.9")

// Shows an ascii library dependency graph (run dependencyTree)
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
