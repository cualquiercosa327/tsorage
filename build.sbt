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

val commonDependencies = Seq(
   "org.scalatest" %% "scalatest" % "3.0.8" % "test",
   "com.typesafe.akka" %% "akka-http" % Version.akka,
   "com.typesafe.akka" %% "akka-stream" % "2.5.23",
   "com.typesafe.akka" %% "akka-http-spray-json" % Version.akka,
   "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.4",
   "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23",
   "com.typesafe.akka" %% "akka-http-testkit" % Version.akka
)

val cassandraDependencies = Seq(
   "com.datastax.oss" % "java-driver-core" % "4.1.0",
   "com.datastax.oss" % "java-driver-query-builder" % "4.1.0",
   "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.0",
)

lazy val common = (project in file("common"))
   //.enablePlugins(DockerPlugin, JavaAppPackaging, GitVersioning)
   .settings(
      name := "common",
      commonSettings,
      libraryDependencies := commonDependencies ++
         cassandraDependencies ++
         Seq(
         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
         "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
   )

lazy val hub = (project in file("hub"))
   //.enablePlugins(DockerPlugin, JavaAppPackaging, GitVersioning)
   .settings(
      name := "hub",
      commonSettings,
      libraryDependencies := commonDependencies ++
         cassandraDependencies ++
         Seq(
         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
         "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
   ).dependsOn(common)

lazy val ingestion = (project in file("ingestion"))
   //.enablePlugins(DockerPlugin, JavaAppPackaging, GitVersioning)
   .settings(
      name := "ingestion",
      commonSettings,
      libraryDependencies := commonDependencies ++
         cassandraDependencies ++
         Seq(
         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
         "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
   ).dependsOn(common)

lazy val processor = (project in file("processor"))
   //.enablePlugins(DockerPlugin, JavaAppPackaging, GitVersioning)
   .settings(
      name := "processor",
      commonSettings,
      libraryDependencies := commonDependencies ++
         cassandraDependencies ++
         Seq(
         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      )
   ).dependsOn(common)

lazy val root = (project in file("."))
   .settings(
      name := "tsorage"
   ).aggregate(common, hub, ingestion, processor)


