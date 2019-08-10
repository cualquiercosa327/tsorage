package be.cetic.tsorage.processor

import java.time.LocalDateTime

/**
  * A package message containing observations.
  */
case class FloatMessage(metric: String, tagset: Map[String, String], values: List[(LocalDateTime, Float)]) extends Serializable
