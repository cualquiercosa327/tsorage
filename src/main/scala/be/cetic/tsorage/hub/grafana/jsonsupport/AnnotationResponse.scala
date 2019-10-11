package be.cetic.tsorage.hub.grafana.jsonsupport

/**
 * A response for the annotation route ("/annotations").
 *
 */
final case class AnnotationResponse(annotations: Seq[AnnotationObject])
