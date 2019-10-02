package be.cetic.tsorage.processor

import java.time.LocalDateTime

import spray.json.JsValue

abstract class Update(
                        val metric: String,
                        val tagset: Map[String, String],
                        val datetime: LocalDateTime,
                        val `type`: String,
                        val value: JsValue
                     )

/**
  * Represents a change in a raw observation.
  */
class RawUpdate(
   metric: String,
   tagset: Map[String, String],
   datetime: LocalDateTime,
   `type`: String,
   value: JsValue
) extends Update(metric, tagset, datetime, `type`, value)

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
class AggUpdate(
   metric: String,
   tagset: Map[String, String],
   val interval: String,
   datetime: LocalDateTime,
   `type`: String,
   value: JsValue,
   val aggregation: String
) extends Update(metric, tagset, datetime, `type`, value)