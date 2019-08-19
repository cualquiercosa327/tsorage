name := "tsorage-processor"

version := "0.1"

scalaVersion := "2.13.0"

libraryDependencies += "com.datastax.oss" % "java-driver-core" % "4.1.0"
libraryDependencies += "com.datastax.oss" % "java-driver-query-builder" % "4.1.0"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"


