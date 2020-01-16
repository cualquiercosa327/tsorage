package be.cetic.tsorage.common.messaging

/**
 * A raw update, simplified in order to only show
 * informations relevant for managing dynamic tag indices.
 */
case class TagUpdate(metric: String, tagname: String, tagvalue: String, tagset: Map[String, String])
