
name := "processor"

version:= "0.1"

mainClass in run := Some("be.cetic.tsorage.processor.Main")
mainClass in Compile := Some("be.cetic.tsorage.processor.Main")
mainClass in packageBin := Some("be.cetic.tsorage.processor.Main")

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)