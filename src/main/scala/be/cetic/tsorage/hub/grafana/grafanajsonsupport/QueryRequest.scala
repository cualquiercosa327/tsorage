package be.cetic.tsorage.hub.grafana.grafanajsonsupport

/**
 * A request for the query route ("/query").
 *
 */
final case class QueryRequest(targets: Seq[Target], range: TimeRange,
                              intervalMs: Option[Long], maxDataPoints: Option[Int])
