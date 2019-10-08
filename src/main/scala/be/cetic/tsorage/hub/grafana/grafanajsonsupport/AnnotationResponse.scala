package be.cetic.tsorage.hub.grafana.grafanajsonsupport

/**
 * A response for the annotation route ("/annotations").
 *
 */
final case class AnnotationResponse(annotations: Seq[AnnotationObject])
