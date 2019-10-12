import Dependencies.Version

name := "tsorage"

version := "0.1"

scalaVersion := "2.12.10"

val commonSettings = Seq(
   organization := "cetic",
   version := "1.0.0",
   scalaVersion := "2.12.10"
)

val akkaVersion = "10.1.10"

lazy val hub = (project in file("hub"))
   //.enablePlugins(DockerPlugin, JavaAppPackaging, GitVersioning)
   .settings(
      name := "hub",
      commonSettings,
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
   )

lazy val ingestion = (project in file("ingestion"))
   //.enablePlugins(DockerPlugin, JavaAppPackaging, GitVersioning)
   .settings(
      name := "ingestion",
      commonSettings,
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
   )

lazy val processor = (project in file("processor"))
   //.enablePlugins(DockerPlugin, JavaAppPackaging, GitVersioning)
   .settings(
      name := "processor",
      commonSettings,
      libraryDependencies := Seq(
         "org.scalatest" %% "scalatest" % "3.0.8" % "test",
         "com.typesafe.akka" %% "akka-stream" % "2.5.23",
         "com.typesafe.akka" %% "akka-http-spray-json" % Version.akka,
         "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.4",
         "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23",

         "com.datastax.oss" % "java-driver-core" % "4.1.0",
         "com.datastax.oss" % "java-driver-query-builder" % "4.1.0",
         "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.0",

         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      )
   )

lazy val root = (project in file("."))
   .settings(
      name := "tsorage"
   ).aggregate(hub, ingestion, processor)


