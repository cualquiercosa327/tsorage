package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.common.messaging.Observation

/**
 * An observation with its support.
 */
case class SupportedObservation(obs: Observation, support: DataTypeSupport[_])
