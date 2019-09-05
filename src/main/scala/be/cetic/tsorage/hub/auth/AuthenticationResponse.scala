package be.cetic.tsorage.hub.auth

import java.time.LocalDateTime

/**
 * The representation of an authenticate TSorage user.
 */
case class AuthenticationResponse(id: Int, name: String, expiracy: Option[LocalDateTime])
