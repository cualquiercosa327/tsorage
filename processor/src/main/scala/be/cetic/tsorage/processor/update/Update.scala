package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

import be.cetic.tsorage.common.TimeSeries
import spray.json.JsValue

/**
  * A representation of an (raw or aggregated) event.
  * That corresponds to a measure to be inserted or updated in the database.
  * @param ts        The updated time series.
  * @param datetime  The instant associated with the observation.
  * @param `type`    A representation of the type of value observed.
  * @param value     The observed value.
  */
case class Update(
                    ts: TimeSeries,
                    datetime: LocalDateTime,
                    `type`: String,
                    value: JsValue
                 )



