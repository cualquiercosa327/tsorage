package be.cetic.tsorage.common.messaging

import java.time.LocalDateTime

/**
 * The representation of an authenticate TSorage user.
 */
case class AuthenticationResponse(id: Int, name: String, expiracy: Option[LocalDateTime])
