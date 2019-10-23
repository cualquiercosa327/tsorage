
name := "processor"

version:= "0.1"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)