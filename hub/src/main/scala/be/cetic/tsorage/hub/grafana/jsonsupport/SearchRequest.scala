package be.cetic.tsorage.hub.grafana.jsonsupport

/**
 * A request for the search route ("/search").
 *
 */
final case class SearchRequest(target: Option[String])
