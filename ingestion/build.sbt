
name := "ingestion"

version:= "0.1"

mainClass in(Compile, run) := Some("be.cetic.tsorage.ingestion.http.HTTPInterface")
mainClass in(Compile, packageBin) := Some("be.cetic.tsorage.ingestion.http.HTTPInterface")



PB.protoSources in Compile := Seq((baseDirectory in ThisBuild).value /"common" /  "src"/ "main" / "protobuf")

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value /// "protos",
)







