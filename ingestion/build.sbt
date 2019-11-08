
name := "ingestion"

version:= "0.1"

mainClass in run := Some("be.cetic.tsorage.ingestion.http.HTTPInterface")
mainClass in Compile := Some("be.cetic.tsorage.ingestion.http.HTTPInterface")
mainClass in packageBin := Some("be.cetic.tsorage.ingestion.http.HTTPInterface")

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)