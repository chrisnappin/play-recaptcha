resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0")

// The PGP plugin (for signing sonatype releases)
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
