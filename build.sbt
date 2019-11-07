import Dependencies.Version
import com.typesafe.sbt.packager.docker.Cmd
import sbt.Keys.libraryDependencies

name := "tsorage"

version := "0.1"

scalaVersion := "2.12.10"

val commonSettings = Seq(
   organization := "cetic",
   version := "1.0.0",
   scalaVersion := "2.12.10",
   // Docker information.
   //dockerRepository := Some("ceticasbl/tsorage")
   dockerBaseImage := "openjdk:12-alpine",
   dockerUpdateLatest := true,
   //dockerAlias := DockerAlias(dockerRepository.value, dockerUsername.value, name.value),
   dockerUsername := Some("ceticasbl"),
   dockerCommands ++= Seq(
     Cmd("USER", "root"),
     Cmd("RUN", "apk", "add", "--no-cache", "bash"),
     Cmd("ADD", "https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh", "."),
     Cmd("RUN", "chmod", "+x", "wait-for-it.sh")
   )
  )

PB.protoSources in Compile := Seq((baseDirectory in ThisBuild).value /"common" /  "src"/ "main" / "protobuf")
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value / "protos",
)


//scalapb.compiler.Version.scalapbVersion
val akkaVersion = "10.1.10"

val commonDependencies = Seq(
   "org.scalatest" %% "scalatest" % "3.0.8" % "test",
   "com.typesafe.akka" %% "akka-http" % Version.akka,
   "com.typesafe.akka" %% "akka-stream" % "2.5.23",
   "com.typesafe.akka" %% "akka-http-spray-json" % Version.akka,
   "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.4",
   "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23",
   "com.typesafe.akka" %% "akka-http-testkit" % Version.akka,
   "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.typesafe.play" %% "play-json" % "2.7.4",
   "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
   "com.thesamet.scalapb" %% "scalapb-runtime" % "0.9.4"


)




val cassandraDependencies = Seq(
   "com.datastax.oss" % "java-driver-core" % "4.1.0",
   "com.datastax.oss" % "java-driver-query-builder" % "4.1.0",
   "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.0"
)

lazy val common = (project in file("common"))
   .enablePlugins(DockerPlugin, JavaAppPackaging)
   .settings(
      name := "tsorage-common",
      packageName := "tsorage-common",
      commonSettings,
      libraryDependencies := commonDependencies ++
         cassandraDependencies ++
         Seq(
         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
         "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
   )

lazy val hub = (project in file("hub"))
   .enablePlugins(DockerPlugin, JavaAppPackaging)
   .settings(
      name := "tsorage-hub",
      packageName := "tsorage-hub",
      commonSettings,
      libraryDependencies := commonDependencies ++
         cassandraDependencies ++
         Seq(
         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
         "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
   ).dependsOn(common)

lazy val ingestion = (project in file("ingestion"))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
   .settings(
      name := "tsorage-ingestion",
      packageName := "tsorage-ingestion",
      commonSettings,
      libraryDependencies := commonDependencies ++
         cassandraDependencies ++
         Seq(
         "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
         "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
   ).dependsOn(common)


lazy val processor = (project in file("processor"))
   .enablePlugins(DockerPlugin, JavaAppPackaging)
   .settings(
      name := "tsorage-processor",
      packageName := "tsorage-processor",
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


