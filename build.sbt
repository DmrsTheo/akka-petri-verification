name := "akka-petri-verification"
version := "1.0.0"
scalaVersion := "2.13.12"

lazy val akkaVersion = "2.8.5"

libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor"       % akkaVersion,

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

  // Test
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

// Forking for Akka
fork := true
connectInput := true
scalacOptions ++= Seq("-encoding", "utf8")
javaOptions ++= Seq("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8")
