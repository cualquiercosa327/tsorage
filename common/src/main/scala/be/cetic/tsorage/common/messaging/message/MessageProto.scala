// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package be.cetic.tsorage.common.messaging.message

object MessageProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq(
    com.google.protobuf.timestamp.TimestampProto
  )
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      be.cetic.tsorage.common.messaging.message.MessagePB
    )
  private lazy val ProtoBytes: Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """Cg1tZXNzYWdlLnByb3RvEiFiZS5jZXRpYy50c29yYWdlLmNvbW1vbi5tZXNzYWdpbmcaH2dvb2dsZS9wcm90b2J1Zi90aW1lc
  3RhbXAucHJvdG8i0AMKCU1lc3NhZ2VQQhIjCgZtZXRyaWMYASABKAlCC+I/CBIGbWV0cmljUgZtZXRyaWMSXQoGdGFnc2V0GAIgA
  ygLMjguYmUuY2V0aWMudHNvcmFnZS5jb21tb24ubWVzc2FnaW5nLk1lc3NhZ2VQQi5UYWdzZXRFbnRyeUIL4j8IEgZ0YWdzZXRSB
  nRhZ3NldBIdCgR0eXBlGAMgASgJQgniPwYSBHR5cGVSBHR5cGUSVwoGdmFsdWVzGAQgAygLMjIuYmUuY2V0aWMudHNvcmFnZS5jb
  21tb24ubWVzc2FnaW5nLk1lc3NhZ2VQQi5WYWx1ZUIL4j8IEgZ2YWx1ZXNSBnZhbHVlcxp2CgVWYWx1ZRJFCghkYXRldGltZRgBI
  AEoCzIaLmdvb2dsZS5wcm90b2J1Zi5UaW1lc3RhbXBCDeI/ChIIZGF0ZXRpbWVSCGRhdGV0aW1lEiYKB3BheWxvYWQYAiABKAxCD
  OI/CRIHcGF5bG9hZFIHcGF5bG9hZBpPCgtUYWdzZXRFbnRyeRIaCgNrZXkYASABKAlCCOI/BRIDa2V5UgNrZXkSIAoFdmFsdWUYA
  iABKAlCCuI/BxIFdmFsdWVSBXZhbHVlOgI4AUIjCiFiZS5jZXRpYy50c29yYWdlLmNvbW1vbi5tZXNzYWdpbmdiBnByb3RvMw=="""
      ).mkString)
  lazy val scalaDescriptor: _root_.scalapb.descriptors.FileDescriptor = {
    val scalaProto = com.google.protobuf.descriptor.FileDescriptorProto.parseFrom(ProtoBytes)
    _root_.scalapb.descriptors.FileDescriptor.buildFrom(scalaProto, dependencies.map(_.scalaDescriptor))
  }
  lazy val javaDescriptor: com.google.protobuf.Descriptors.FileDescriptor = {
    val javaProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.parseFrom(ProtoBytes)
    com.google.protobuf.Descriptors.FileDescriptor.buildFrom(javaProto, Array(
      com.google.protobuf.timestamp.TimestampProto.javaDescriptor
    ))
  }
  @deprecated("Use javaDescriptor instead. In a future version this will refer to scalaDescriptor.", "ScalaPB 0.5.47")
  def descriptor: com.google.protobuf.Descriptors.FileDescriptor = javaDescriptor
}