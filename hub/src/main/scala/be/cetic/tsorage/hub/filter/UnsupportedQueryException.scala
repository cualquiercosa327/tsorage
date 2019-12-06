package be.cetic.tsorage.hub.filter

/**
 * The specified filter is unsupported.
 */
case class UnsupportedQueryException(f: Filter) extends RuntimeException
