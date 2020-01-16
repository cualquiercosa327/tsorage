package be.cetic.tsorage.processor

import be.cetic.tsorage.common.messaging.Message

case class UnknownTypeMessageError(message: Message) extends Error
{

}
