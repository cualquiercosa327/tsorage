package be.cetic.tsorage.hub.grafana.grafanajsonsupport

/**
 * A response for the search route ("/search").
 *
 */
final case class SearchResponse(targets: Seq[String])
