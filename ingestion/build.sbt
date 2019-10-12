name := "ingestion"

version:= "0.1"

mainClass in(Compile, run) := Some("be.cetic.tsorage.ingestion.http.HTTPInterface")
mainClass in(Compile, packageBin) := Some("be.cetic.tsorage.ingestion.http.HTTPInterface")