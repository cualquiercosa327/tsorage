package be.cetic.tsorage.hub.grafana.grafanajsonsupport

/**
 * An annotation.
 *
 */
final case class Annotation(name: String, enable: Boolean, datasource: String, iconColor: Option[String],
                            query: Option[String])
