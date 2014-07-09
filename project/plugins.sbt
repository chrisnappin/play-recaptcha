resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0")

// The PGP plugin (for signing sonatype releases)
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")
