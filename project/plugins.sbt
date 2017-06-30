resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.0")

// The PGP plugin (for signing sonatype releases)
addSbtPlugin("com.jsuereth" %% "sbt-pgp" % "1.1.0-M1")

// The scoverage plugin (measures statement coverage for unit tests)
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.5.0")
