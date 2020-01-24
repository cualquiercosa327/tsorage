package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime

/**
 * A type associated with a date.
 */
case class DatedType[T](datetime: LocalDateTime, value: T)
