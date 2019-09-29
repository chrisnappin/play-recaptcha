//resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.3")

// The PGP plugin (for signing sonatype releases)
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

// The scoverage plugin (measures statement coverage for unit tests)
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.6.0")

// Shows an ascii library dependency graph (run dependencyTree_
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")