package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

import spray.json.JsValue

/**
  * A representation of an (raw or aggregated) event.
  * That corresponds to a measure to be inserted or updated in the database.
  * @param metric    A identifier for the object being observed. Typically, that correspond to the ID of a sensor.
  * @param tagset    A set of properties associated with this observation.
  * @param datetime  The instant associated with the observation.
  * @param `type`    A representation of the type of value observed.
  * @param value     The observed value.
  */
case class Update(
                    metric: String,
                    tagset: Map[String, String],
                    datetime: LocalDateTime,
                    `type`: String,
                    value: JsValue
                 )



