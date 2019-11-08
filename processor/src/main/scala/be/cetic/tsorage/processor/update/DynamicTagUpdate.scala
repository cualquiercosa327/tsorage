package be.cetic.tsorage.processor.update

/**
 * A raw update, simplified in order to only show
 * informations relevant for managing dynamic tag indices.
 */
case class DynamicTagUpdate(metric: String, tagname: String, tagvalue: String, tagset: Map[String, String])
