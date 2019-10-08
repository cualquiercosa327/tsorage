package be.cetic.tsorage.hub.grafana.grafanajsonsupport

/**
 * A response for the query route ("/query").
 *
 */
final case class QueryResponse(dataPointsSeq: Seq[DataPoints])
