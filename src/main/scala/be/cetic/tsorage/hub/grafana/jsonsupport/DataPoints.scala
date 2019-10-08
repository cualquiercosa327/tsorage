package be.cetic.tsorage.hub.grafana.jsonsupport

/**
 * Data points for a single target.
 *
 */
final case class DataPoints(target: String, datapoints: Seq[(BigDecimal, Long)])
