import Dependencies.Version

name := "common"

version:= "0.1"

//PB.protoSources in Compile := Seq((baseDirectory in ThisBuild).value)// /"common" /  "src"/ "main" / "protobuf")
//PB.protoSources in Compile += target.value / "protobuf_external" / "com" / "thesamet"


PB.targets in Compile := Seq(
   scalapb.gen() -> (sourceManaged in Compile).value
)

PB.protoSources in Compile := Seq(
   (baseDirectory in ThisBuild).value /"common" /  "src"/ "main" / "protobuf"
)

PB.targets in Compile := Seq(
   scalapb.gen() -> (sourceManaged in Compile).value // "protos",
)
