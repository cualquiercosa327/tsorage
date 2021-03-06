package be.cetic.tsorage.hub.grafana.jsonsupport

/**
 * An annotation object (that is, an annotation with a title and a time).
 *
 */
final case class AnnotationObject(annotation: Annotation, title: String, time: Long)
