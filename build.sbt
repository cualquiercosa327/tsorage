name := "tsorage-hub"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.10"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.4"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.1.10"

libraryDependencies += "com.datastax.oss" % "java-driver-core" % "4.1.0"
libraryDependencies += "com.datastax.oss" % "java-driver-query-builder" % "4.1.0"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.0"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
