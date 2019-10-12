name := "hub"

version:= "0.1"

mainClass in(Compile, run) := Some("be.cetic.tsorage.hub.Site")
mainClass in(Compile, packageBin) := Some("be.cetic.tsorage.hub.Site")