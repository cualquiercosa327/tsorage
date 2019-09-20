package be.cetic.tsorage.processor

import java.time.LocalDateTime

/**
  * A package message containing observations.
  */
case class Message[T](metric: String, tagset: Map[String, String], values: List[(LocalDateTime, T)]) extends Serializable

case class Observation[T](metric: String, tagset: Map[String, String], datetime: LocalDateTime, value: T) extends Serializable