package be.cetic.tsorage.ingestion.message

import java.time.LocalDateTime

/**
 * The representation of an authenticate TSorage user.
 *
 * TODO : The class also exists in the Hub repository. Common concepts should be declared only once.
 */
case class User(id: Int, name: String, expiracy: Option[LocalDateTime])
