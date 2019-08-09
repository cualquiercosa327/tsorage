package be.cetic.tsorage.processor

import java.time.LocalDateTime

/**
  * A package message containing observations.
  */
case class FloatMessage(sensor: String, tags: Map[String, String], values: List[(LocalDateTime, Float)]) extends Serializable
