package be.cetic.tsorage.hub.auth.backend

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AuthenticationResponse

/**
 * A backend for the authentication, based on hardcoded values
 */
object MockupAuthenticationBackend extends AuthenticationBackend
{
   private val users: Map[String, AuthenticationResponse] = Map(
      "4b8639ed-0e90-4b3f-8a45-e87c22d17887" -> AuthenticationResponse(421, "Mathieu Goeminne", None),
      "6bd66ef491424668aa0175e6ad1b2a99" -> AuthenticationResponse(422, "Nico Salamone", None),
      "5950effa-bfb1-42b1-9d55-e1f07261fd38" -> AuthenticationResponse(1337, "Boris Vian", Some(LocalDateTime.now())),
      "ed1e6666-10f8-4cf5-bed9-610102457aa9" -> AuthenticationResponse(876543, "JRR Tolkien", None),
      "b78d379a9f58674acc1acf1131cba62d" -> AuthenticationResponse(4242, "DD Test", None)
   )

   override def check(token: String): Option[AuthenticationResponse] = users.get(token)
}
