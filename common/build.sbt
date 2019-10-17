import Dependencies.Version

name := "common"

version:= "0.1"

libraryDependencies := Seq(
   "org.scalatest" %% "scalatest" % "3.0.8" % "test",
   "com.typesafe.akka" %% "akka-http" % Version.akka,
   "com.typesafe.akka" %% "akka-stream" % "2.5.23",
   "com.typesafe.akka" %% "akka-http-spray-json" % Version.akka,
   "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.4",
   "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23",
   "com.typesafe.akka" %% "akka-http-testkit" % Version.akka,

   "com.datastax.oss" % "java-driver-core" % "4.1.0",
   "com.datastax.oss" % "java-driver-query-builder" % "4.1.0",
   "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.0",

   "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
   "ch.qos.logback" % "logback-classic" % "1.2.3"
)