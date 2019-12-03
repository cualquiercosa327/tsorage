
name := "processor"

version:= "0.1"

mainClass in run := Some("be.cetic.tsorage.processor.Main")
mainClass in Compile := Some("be.cetic.tsorage.processor.Main")
mainClass in packageBin := Some("be.cetic.tsorage.processor.Main")

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

PB.protoSources in Compile := Seq((baseDirectory in ThisBuild).value /"common" /  "src"/ "main" / "protobuf")

PB.targets in Compile := Seq( scalapb.gen() -> (sourceManaged in Compile).value /// "protos",
)
