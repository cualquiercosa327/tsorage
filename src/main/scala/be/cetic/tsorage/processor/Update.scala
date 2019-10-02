package be.cetic.tsorage.processor

import java.time.LocalDateTime

import spray.json.JsValue

/**
  * Represents a change in a raw observation.
  */
case class RawUpdate(
                       metric: String,
                       tagset: Map[String, String],
                       datetime: LocalDateTime,
                       `type`: String,
                       value: JsValue
                    )

/**
  * Represents a change in an aggregated observation.
  * @param metric
  * @param tagset
  * @param interval
  * @param datetime
  * @param `type`
  * @param value
  * @param aggregation
  */
case class AggUpdate(
                       metric: String,
                       tagset: Map[String, String],
                       interval: String,
                       datetime: LocalDateTime,
                       `type`: String,
                       value: JsValue,
                       aggregation: String
                    )