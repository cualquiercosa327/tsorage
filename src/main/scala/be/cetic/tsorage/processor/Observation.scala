package be.cetic.tsorage.processor

import java.time.LocalDateTime

import be.cetic.tsorage.processor.datatype.DataTypeSupport

/**
  * An atomic observation.
  */
case class Observation[T](
                            metric: String,
                            tagset: Map[String, String],
                            datetime: LocalDateTime,
                            value: T,
                            support: DataTypeSupport[T]
                         ) extends Serializable
