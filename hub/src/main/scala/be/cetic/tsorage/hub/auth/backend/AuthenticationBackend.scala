package be.cetic.tsorage.hub.auth.backend

import be.cetic.tsorage.common.messaging.AuthenticationResponse
import com.typesafe.config.Config

/**
 * A generic trait for classes managing authentication backends.
 */
trait AuthenticationBackend
{
   /**
    * Check whether an API token validly corresponds to a TSorage user.
    * @param token The token to be checked.
    * @return The user validly associated with token.
    */
   def check(token: String): Option[AuthenticationResponse]
}

object AuthenticationBackend
{
   def apply(conf: Config) = conf.getString("name") match {
      case "mockup" => MockupAuthenticationBackend
   }
}