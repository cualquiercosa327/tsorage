package be.cetic.tsorage.processor

import java.time.LocalDateTime

import be.cetic.tsorage.processor.aggregator.data.{DataAggregation, SupportedValue}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue}
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