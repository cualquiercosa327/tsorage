package be.cetic.tsorage.hub.grafana.jsonsupport

/**
 * A response for the search route ("/search").
 *
 */
final case class SearchResponse(targets: Seq[String])
